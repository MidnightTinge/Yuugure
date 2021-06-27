package com.mtinge.TagTokenizer;

import com.mtinge.TagTokenizer.lexer.LexToken;
import com.mtinge.TagTokenizer.lexer.Lexer;
import com.mtinge.TagTokenizer.tokenizer.TermModifier;
import com.mtinge.TagTokenizer.tokenizer.Tokenizer;
import com.mtinge.TagTokenizer.tokenizer.TokenizerToken;
import com.mtinge.TagTokenizer.tokenizer.TokenizerToken.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class TokenizerTest {
  @Test
  void handlesTerms() throws SyntaxError {
    LinkedList<TokenizerToken> tokens;
    TokenizerToken token;

    tokens = Tokenizer.tokenize(Lexer.lex("abc def"));
    assertEquals(2, tokens.size());

    token = tokens.removeFirst();
    assertEquals(Type.TERM, token.type);
    assertEquals("abc", token.value);

    token = tokens.removeFirst();
    assertEquals(Type.TERM, token.type);
    assertEquals("def", token.value);
  }

  @Test
  void handlesLeadingGroups() throws SyntaxError {
    LinkedList<TokenizerToken> tokens;
    TokenizerToken token;

    tokens = Tokenizer.tokenize(Lexer.lex("(abc) def"));
    assertEquals(2, tokens.size());

    token = tokens.removeFirst();
    assertEquals(Type.GROUP, token.type);
    assertNull(token.value);
    assertNotNull(token.children);
    assertEquals(1, token.children.size());

    token = token.children.removeFirst();
    assertEquals(Type.TERM, token.type);
    assertEquals("abc", token.value);

    token = tokens.removeFirst();
    assertEquals(Type.TERM, token.type);
    assertEquals("def", token.value);
  }

  @Test
  void handlesMixedGroups() throws SyntaxError {
    LinkedList<TokenizerToken> tokens;
    TokenizerToken token;

    tokens = Tokenizer.tokenize(Lexer.lex("abc (def) ghi"));
    assertEquals(3, tokens.size());

    token = tokens.removeFirst();
    assertEquals(Type.TERM, token.type);
    assertEquals("abc", token.value);

    token = tokens.removeFirst();
    assertEquals(Type.GROUP, token.type);
    assertNull(token.value);
    assertNotNull(token.children);
    assertEquals(1, token.children.size());
    token = token.children.removeFirst();
    assertEquals(Type.TERM, token.type);
    assertEquals("def", token.value);

    token = tokens.removeFirst();
    assertEquals(Type.TERM, token.type);
    assertEquals("ghi", token.value);
  }

  @Test
  void handlesTrailingGroups() throws SyntaxError {
    LinkedList<TokenizerToken> tokens;
    TokenizerToken token;

    tokens = Tokenizer.tokenize(Lexer.lex("abc (def)"));
    assertEquals(2, tokens.size());

    token = tokens.removeFirst();
    assertEquals(Type.TERM, token.type);
    assertEquals("abc", token.value);

    token = tokens.removeFirst();
    assertEquals(Type.GROUP, token.type);
    assertNull(token.value);
    assertNotNull(token.children);
    assertEquals(1, token.children.size());

    token = token.children.removeFirst();
    assertEquals(Type.TERM, token.type);
    assertEquals("def", token.value);
  }

  @Test
  void handlesMultipleTopLevelGroups() throws SyntaxError {
    var tokens = Tokenizer.tokenize(Lexer.lex("(a) b (c) d (e) (f)"));
    assertEquals(6, tokens.size());

    assertEquals(Type.GROUP, tokens.removeFirst().type);
    assertEquals(Type.TERM, tokens.removeFirst().type);
    assertEquals(Type.GROUP, tokens.removeFirst().type);
    assertEquals(Type.TERM, tokens.removeFirst().type);
    assertEquals(Type.GROUP, tokens.removeFirst().type);
    assertEquals(Type.GROUP, tokens.removeFirst().type);
  }

  @Test
  void handlesNestedGroups() throws SyntaxError {
    TokenizerToken token;
    var tokens = Tokenizer.tokenize(Lexer.lex("((a) b (c)) d"));

    assertEquals(2, tokens.size());

    token = tokens.removeFirst();
    assertEquals(Type.GROUP, token.type);
    assertNotNull(token.children);
    assertEquals(3, token.children.size());

    assertEquals(Type.GROUP, token.children.removeFirst().type);
    assertEquals(Type.TERM, token.children.removeFirst().type);
    assertEquals(Type.GROUP, token.children.removeFirst().type);

    token = tokens.removeFirst();
    assertEquals(Type.TERM, token.type);
  }

  @Test
  void handlesMultipleNestedGroups() throws SyntaxError {
    TokenizerToken token;
    var tokens = Tokenizer.tokenize(Lexer.lex("one (two (three (four five) six))"));
    /*
     * TERM: one
     * GROUP: [
     *   TERM: two,
     *   GROUP: [
     *     TERM: three,
     *     GROUP: [four, five],
     *     TERM: six,
     *   ],
     * ]
     */

    assertEquals(2, tokens.size());

    var one = tokens.removeFirst();
    assertEquals(Type.TERM, one.type);
    assertEquals("one", one.value);

    var onesGroup = tokens.removeFirst();
    assertEquals(Type.GROUP, onesGroup.type);
    assertNotNull(onesGroup.children);
    assertEquals(2, onesGroup.children.size());

    var two = onesGroup.children.removeFirst();
    assertEquals(Type.TERM, two.type);
    assertEquals("two", two.value);

    var twosGroup = onesGroup.children.removeFirst();
    assertEquals(Type.GROUP, twosGroup.type);
    assertNotNull(twosGroup.children);
    assertEquals(3, twosGroup.children.size());

    var three = twosGroup.children.removeFirst();
    assertEquals(Type.TERM, three.type);
    assertEquals("three", three.value);

    var threesGroup = twosGroup.children.removeFirst();
    assertEquals(Type.GROUP, threesGroup.type);
    assertNotNull(threesGroup.children);
    assertEquals(2, threesGroup.children.size());

    var four = threesGroup.children.removeFirst();
    assertEquals(Type.TERM, four.type);
    assertEquals("four", four.value);

    var five = threesGroup.children.removeFirst();
    assertEquals(Type.TERM, five.type);
    assertEquals("five", five.value);

    var six = twosGroup.children.removeFirst();
    assertEquals(Type.TERM, six.type);
    assertEquals("six", six.value);
  }

  @Test
  void handlesGroupModifier() throws SyntaxError {
    LinkedList<TokenizerToken> tokens;
    TokenizerToken token;

    tokens = Tokenizer.tokenize(Lexer.lex("-(a) +(b) ~(c)"));
    assertEquals(3, tokens.size());

    token = tokens.removeFirst();
    assertEquals(Type.GROUP, token.type);
    assertEquals(TermModifier.NOT, token.modifier);

    token = tokens.removeFirst();
    assertEquals(Type.GROUP, token.type);
    assertEquals(TermModifier.AND, token.modifier);

    token = tokens.removeFirst();
    assertEquals(Type.GROUP, token.type);
    assertEquals(TermModifier.OR, token.modifier);
  }

  @Test
  void throwsForInvalidGroupCloser() {
    assertThrows(SyntaxError.class, () -> {
      Tokenizer.tokenize(Lexer.lex("ab)cd"));
    });
  }

  @Test
  void throwsForUnbalancedGroup() {
    assertThrows(SyntaxError.class, () -> {
      Tokenizer.tokenize(Lexer.lex("a(b(c)d"));
    });
  }

  @Test
  void throwsForUnknownModifier() {
    assertThrows(SyntaxError.class, () -> {
      int i = 0;
      Tokenizer.tokenize(
        new LinkedList<>(
          List.of(
            new LexToken(LexToken.Type.CHAR, i++, 'a'),
            new LexToken(LexToken.Type.MODIFIER, i++, '\1'), // invalid modifier
            new LexToken(LexToken.Type.END, i++, '\0')
          )
        )
      );
    });
  }

  @Test
  void throwsForEmptyGroup() {
    assertThrows(SyntaxError.class, () -> {
      Tokenizer.tokenize(Lexer.lex("()"));
    });
  }

  @Test
  void throwsForModifyingModifier() {
    assertThrows(SyntaxError.class, () -> {
      Tokenizer.tokenize(Lexer.lex("-+a"));
    });
  }

  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = 3)
  void doesntHangOnUnknownToken() throws SyntaxError {
    Tokenizer.tokenize(
      new LinkedList<>(
        List.of(
          new LexToken(null, 0, '\0'),
          new LexToken(LexToken.Type.END, 0, '\0')
        )
      )
    );
  }

}
