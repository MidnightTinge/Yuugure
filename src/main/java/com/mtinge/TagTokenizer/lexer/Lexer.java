package com.mtinge.TagTokenizer.lexer;

import com.mtinge.TagTokenizer.lexer.LexToken.Type;

import java.util.LinkedList;

/**
 * A lexer for Tags. Lexing breaks input into consumable tokens for further processing.
 */
public class Lexer {
  public static LinkedList<LexToken> lex(String input) {
    var tokens = new LinkedList<LexToken>();

    int len = input.length();
    int i = 0;
    char c;
    boolean escapedOpener = false;
    while (i < len) {
      c = input.charAt(i);
      if (c == '\\') { // escape
        tokens.add(new LexToken(Type.CHAR_ESCAPED, ++i, input.charAt(i++))); // mind the `i` positional requirements when modifying.
        continue;
      }
      if (c == '-' || c == '+' || c == '~') { // modifier
        tokens.add(new LexToken(Type.MODIFIER, i++, c));
        continue;
      }
      if (c == '(') { // group start
        LexToken lastToken = !tokens.isEmpty() ? tokens.getLast() : null;
        if (lastToken != null && ((lastToken.type == Type.CHAR || lastToken.type == Type.CHAR_ESCAPED) && lastToken.value == '_')) {
          // handle the edge-case `character_name_(series_name)` where it's all one tag.
          escapedOpener = true;
          tokens.add(new LexToken(Type.CHAR_ESCAPED, i, input.charAt(i++)));
        } else {
          escapedOpener = false;
          tokens.add(new LexToken(Type.GROUP_START, i++, c));
        }
        continue;
      }
      if (c == ')') { // group end
        if (escapedOpener) {
          // handle the edge-case `character_name_(series_name)` where it's all one tag.
          tokens.add(new LexToken(Type.CHAR_ESCAPED, i, input.charAt(i++)));
        } else {
          tokens.add(new LexToken(Type.GROUP_END, i++, c));
        }
        escapedOpener = false;
        continue;
      }
      if (c == ' ') {
        tokens.add(new LexToken(Type.SEPARATOR, i++, c));
        continue;
      }
      tokens.add(new LexToken(Type.CHAR, i++, c));
    }

    tokens.add(new LexToken(Type.END, i, '\0'));
    return tokens;
  }
}
