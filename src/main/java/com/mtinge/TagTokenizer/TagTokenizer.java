package com.mtinge.TagTokenizer;

import com.mtinge.TagTokenizer.lexer.LexToken;
import com.mtinge.TagTokenizer.lexer.Lexer;
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
}
