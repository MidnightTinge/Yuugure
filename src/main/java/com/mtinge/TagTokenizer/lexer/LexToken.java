package com.mtinge.TagTokenizer.lexer;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LexToken {
  public enum Type {
    MODIFIER,
    CHAR_ESCAPED,
    GROUP_START,
    GROUP_END,
    SEPARATOR,
    CHAR,
    END
  }

  public final LexToken.Type type;
  public final int index;
  public final char value;
}
