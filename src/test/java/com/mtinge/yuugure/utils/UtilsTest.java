package com.mtinge.yuugure.utils;

import com.mtinge.yuugure.core.Utils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UtilsTest {
  @Test
  @DisplayName("Converts to correct hex string")
  public void correctHexString() {
    assertEquals("010203", Utils.toHex(new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0x03}));
    assertEquals("4a4b4c", Utils.toHex("JKL".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  @DisplayName("Gets correct token length")
  public void correctTokenLength() {
    assertEquals(32, Utils.token(16).length());
    assertEquals(64, Utils.token(32).length());
  }
}
