package com.mtinge.TagTokenizer;

import com.mtinge.TagTokenizer.lexer.LexToken;
import com.mtinge.TagTokenizer.lexer.Lexer;
import com.mtinge.TagTokenizer.tokenizer.TermModifier;
import com.mtinge.TagTokenizer.tokenizer.Tokenizer;
import com.mtinge.TagTokenizer.tokenizer.TokenizerToken;

import java.util.LinkedList;

public class TagTokenizer {
  public static LinkedList<LexToken> lex(String input) {
    return Lexer.lex(input);
  }

  public static LinkedList<TokenizerToken> parse(String input) throws SyntaxError {
    return parse(lex(input));
  }

  public static LinkedList<TokenizerToken> parse(LinkedList<LexToken> tokens) throws SyntaxError {
    return Tokenizer.tokenize(tokens);
  }

  @SuppressWarnings("ConstantConditions")
  private static void _explain(LinkedList<TokenizerToken> tokens, int indent, StringBuilder sb) {
    for (var token : tokens) {
      sb.append(indent(indent)).append(token.type);
      if (token.modifier != TermModifier.AND) {
        sb.append(" {modifier=").append(token.modifier.toString().toLowerCase()).append("}");
      }
      if (token.type.equals(TokenizerToken.Type.TERM)) {
        sb.append(": ").append(token.value).append("\n");
      } else {
        sb.append("\n");
        // It is illegal to have an empty group and .children will never be null when the type is
        // group.
        _explain(token.children, indent + 1, sb);
      }
    }
  }

  public static String explain(LinkedList<TokenizerToken> tokens) {
    var sb = new StringBuilder();

    _explain(tokens, 0, sb);

    return sb.toString();
  }

  private static String indent(int amount) {
    return "\t".repeat(Math.max(0, amount));
  }
}
