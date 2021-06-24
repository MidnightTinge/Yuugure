package com.mtinge.AcceptParser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MimeTest {
  @Test
  public void parsesCorrectly() {
    var parsed = Mime.parse("a/b");
    Assertions.assertEquals("a", parsed.getType());
    Assertions.assertEquals("b", parsed.getSubtype());
  }

  @Test
  public void throwsWhenMalformed() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> Mime.parse("bad"));
  }
}
