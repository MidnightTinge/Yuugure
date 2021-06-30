package com.mtinge.TagTokenizer;

import com.mtinge.TagTokenizer.lexer.LexToken;
import com.mtinge.TagTokenizer.lexer.LexToken.Type;
import com.mtinge.TagTokenizer.lexer.Lexer;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LexerTest {
  @Test
  void injectsEndToken() {
    var lexed = Lexer.lex("a");
    assertEquals(2, lexed.size());
    assertEquals(Type.END, lexed.removeLast().type);
  }

  @Test
  void handlesChars() {
    LexToken token;
    LinkedList<LexToken> lexed;

    lexed = Lexer.lex("abc");
    assertEquals(4, lexed.size());

    token = lexed.removeFirst();
    assertEquals(Type.CHAR, token.type);
    assertEquals('a', token.value);

    token = lexed.removeFirst();
    assertEquals(Type.CHAR, token.type);
    assertEquals('b', token.value);

    token = lexed.removeFirst();
    assertEquals(Type.CHAR, token.type);
    assertEquals('c', token.value);
  }

  @Test
  void handlesEscape() {
    LexToken token;
    LinkedList<LexToken> lexed;

    lexed = Lexer.lex("a\\-b");
    assertEquals(4, lexed.size());

    token = lexed.removeFirst();
    assertEquals(Type.CHAR, token.type);
    assertEquals('a', token.value);

    token = lexed.removeFirst();
    assertEquals(Type.CHAR_ESCAPED, token.type);
    assertEquals('-', token.value);

    token = lexed.removeFirst();
    assertEquals(Type.CHAR, token.type);
    assertEquals('b', token.value);
  }

  @Test
  void handlesModifier() {
    LexToken token;
    LinkedList<LexToken> lexed;

    lexed = Lexer.lex("a-b");
    assertEquals(4, lexed.size());

    token = lexed.removeFirst();
    assertEquals(Type.CHAR, token.type);
    assertEquals('a', token.value);

    token = lexed.removeFirst();
    assertEquals(Type.MODIFIER, token.type);
    assertEquals('-', token.value);

    token = lexed.removeFirst();
    assertEquals(Type.CHAR, token.type);
    assertEquals('b', token.value);
  }

  @Test
  void handlesGroupStart() {
    LexToken token;
    LinkedList<LexToken> lexed;

    lexed = Lexer.lex("a(b");
    assertEquals(4, lexed.size());

    token = lexed.removeFirst();
    assertEquals(Type.CHAR, token.type);
    assertEquals('a', token.value);

    token = lexed.removeFirst();
    assertEquals(Type.GROUP_START, token.type);

    token = lexed.removeFirst();
    assertEquals(Type.CHAR, token.type);
    assertEquals('b', token.value);
  }

  @Test
  void handlesGroupEnd() {
    LexToken token;
    LinkedList<LexToken> lexed;

    lexed = Lexer.lex("a)b");
    assertEquals(4, lexed.size());

    token = lexed.removeFirst();
    assertEquals(Type.CHAR, token.type);
    assertEquals('a', token.value);

    token = lexed.removeFirst();
    assertEquals(Type.GROUP_END, token.type);

    token = lexed.removeFirst();
    assertEquals(Type.CHAR, token.type);
    assertEquals('b', token.value);
  }

  @Test
  void handlesSeparator() {
    LexToken token;
    LinkedList<LexToken> lexed;

    lexed = Lexer.lex(" ");
    assertEquals(2, lexed.size());

    token = lexed.removeFirst();
    assertEquals(Type.SEPARATOR, token.type);
  }

  @Test
  void escapesGroupStartInUnderscoreTag() {
    LexToken token;
    LinkedList<LexToken> lexed;

    lexed = Lexer.lex("a_(b)");
    assertEquals(6, lexed.size());

    token = lexed.removeFirst();
    assertEquals(Type.CHAR, token.type);

    token = lexed.removeFirst();
    assertEquals(Type.CHAR, token.type);

    token = lexed.removeFirst();
    assertEquals(Type.CHAR_ESCAPED, token.type);

    token = lexed.removeFirst();
    assertEquals(Type.CHAR, token.type);

    token = lexed.removeFirst();
    assertEquals(Type.CHAR_ESCAPED, token.type);
  }
}
