package com.mtinge.TagTokenizer.tokenizer;

import com.mtinge.TagTokenizer.SyntaxError;
import com.mtinge.TagTokenizer.lexer.LexToken;

import java.util.LinkedList;
import java.util.List;

/**
 * <p>A Tag tokenizer for composing search queries.</p>
 * <p>
 * Tokenizing happens in two steps. First, we have to break down raw input into lexed tokens. These
 * lexed tokens contain data such as the type of character, the index, and the value.</p>
 * <p>A string like <code>"a\bc -def"</code> would be broken down into something along the lines
 * of:
 * <pre>
 * [
 *  {type: 'char', index: 0, value: 'a'},
 *  {type: 'char_escaped', index: 2, value: 'b'},
 *  {type: 'char', index: 3, value: 'b'},
 *  {type: 'separator', index: 4, value: ' '},
 *  {type: 'modifier', index: 5, value: '-'},
 *  {type: 'char', index: 6, value: 'd'},
 *  {type: 'char', index: 7, value: 'e'},
 *  {type: 'char', index: 8, value: 'f'},
 * ]
 * </pre>
 * This pre-processing is what lets the second step know what's going on. Since we've broken the
 * piece down into characters, processed any escaping, and done anything else necessary, we can now
 * convert things into actionable tokens.
 * </p>
 * <p>
 * Once we have a set of characters, modifiers, and whatever else is needed, we can start processing
 * logical blocks. When we sequencially scan our array from above, we'll eventualy be left with two
 * results:
 * <pre>
 * [
 *   {type: 'TERM', modifiers: 'AND', value: 'abc'},
 *   {type: 'TERM', modifiers: 'NOT', value: 'def'},
 * ]
 * </pre>
 * </p>
 * <p>
 * We can then pass this result off to a search processor that will find posts with the tags "abc"
 * and not "def". Groups follow the same format and are handled just like 'TERM' tokens, however
 * instead of a "value" property they have a "values" (plural) property which contains an array of
 * tokens. For example, with an input string of "abc -(def) ghi", we would receive something like:
 * <pre>
 * [
 *  {type: 'TERM', modifiers: 'AND', value: 'ABC'},
 *  {
 *    type: 'GROUP',
 *    modifiers: 'NOT',
 *    values: [
 *      {type: 'TERM', modifiers: 'AND', value: 'DEF'}
 *    ],
 *  },
 *  {type: 'TERM', modifiers: 'AND', value: 'GHI'},
 * ]
 * </pre>
 * This keeps things relatively relaxed and lets the search processor handle groups however it
 * pleases.
 *
 * @author MidnightTinge
 */
public class Tokenizer {
  private static TermModifier charToModifier(char c) {
    switch (c) {
      case '+' -> {
        return TermModifier.AND;
      }
      case '-' -> {
        return TermModifier.NOT;
      }
      case '~' -> {
        return TermModifier.OR;
      }
    }

    return null;
  }

  public static LinkedList<TagToken> tokenize(LinkedList<LexToken> input) throws SyntaxError {
    LinkedList<TagToken> tokens = new LinkedList<>();
    TermModifier modifier = null;
    LexToken[] lexed = input.toArray(new LexToken[0]);
    int i = 0;

    while (i < lexed.length) {
      LexToken token = lexed[i];

      if (token.type == LexToken.Type.GROUP_END) {
        throw new SyntaxError("Invalid group closer at " + token.index);
      } else if (token.type == LexToken.Type.MODIFIER) {
        if (i > 0 && lexed[i - 1].type == LexToken.Type.MODIFIER) {
          throw new SyntaxError("Illegal modifier at " + token.index + ", cannot modify a modifier.");
        }
        modifier = charToModifier(token.value);
        if (modifier == null) {
          throw new SyntaxError("Unknown modifier at " + token.index + ".");
        }
        i++;
      } else if (token.type == LexToken.Type.GROUP_START) {
        // Reached start of a group, scan until we find group_end or another group_start for
        // nesting.
        int j = i; // Keep track of `i` so we can report an error if necessary.
        int balance = 1; // We're at 1 initially due to our current token being a group_start.

        // We use a nested while loop here to avoid issues with group count. If you recurse here,
        // you then have to scan the resulting object to see how many groups were found to add to
        // `i`. This avoids the double scan.
        while (++j < lexed.length) {
          if (lexed[j].type == LexToken.Type.GROUP_END) {
            // Found a potential group closer, check for balance and process tokens.
            if (--balance == 0) {
              // We've found the final closing parantheses. Recurse down the tokens to get group
              // contents.
              LexToken[] toRecurse = new LexToken[j - (i + 1)];
              System.arraycopy(lexed, i + 1, toRecurse, 0, j - (i + 1));
              LinkedList<TagToken> _parsed = tokenize(new LinkedList<>(List.of(toRecurse)));
              if (!_parsed.isEmpty()) {
                tokens.add(new TagToken(TagToken.Type.GROUP, modifier == null ? TermModifier.AND : modifier, _parsed));
              } else {
                throw new SyntaxError("Invalid/empty group at " + token.index);
              }

              modifier = null; // ensure we reset our modifier
              break; // break out of our group detection loop
            }
          } else if (lexed[j].type == LexToken.Type.GROUP_START) {
            // Found a group opener, keep track of balance and continue
            balance++;
          } else if (lexed[j].type == LexToken.Type.END) {
            if (balance != 0) {
              // We hit EOF before balancing our groups, throw a syntax error
              throw new SyntaxError("Unbalanced group at " + token.index + ". Balance reported: " + balance);
            }
          }
        }
        i = j + 1; // we +1 so we don't encounter group_end at loop head
      } else if (token.type == LexToken.Type.CHAR || token.type == LexToken.Type.CHAR_ESCAPED) {
        // String assumed, consume all chars until separator
        var sb = new StringBuilder();
        do {
          // Consume all sequencial char lexer tokens to create a string
          sb.append(lexed[i++].value);
        } while (i < lexed.length && (lexed[i].type == LexToken.Type.CHAR || lexed[i].type == LexToken.Type.CHAR_ESCAPED));
        tokens.add(new TagToken(TagToken.Type.TERM, modifier == null ? TermModifier.AND : modifier, sb.toString()));
        modifier = null; // ensure modifier resets
      } else if (token.type == LexToken.Type.SEPARATOR) {
        // Separator has no purpose other than to make EndOfTerm. Increment our counter and spin.
        i++;
      } else if (token.type == LexToken.Type.END) {
        // End Of Input
        break;
      } else {
        // Catch-all so that if spec changes but this token isn't recognized we don't infinite loop.
        System.err.println("Unhandled token at index " + token.index);
        i++;
      }
    }

    return tokens;
  }
}
