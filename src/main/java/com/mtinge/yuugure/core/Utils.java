package com.mtinge.yuugure.core;

import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class Utils {
  private static final SecureRandom secureRandom = new SecureRandom();
  private static final char[] HEX = "0123456789abcdef".toCharArray();

  /**
   * Gets the hex representation of a random byte chunk from {@link SecureRandom}. Primarily used
   * for session tokens.
   *
   * @param numBytes The number of bytes to generate. Note that the resulting string will be
   *   {@code 2*numBytes} since we're returning the hex representation.
   *
   * @return The hex representation of a secured random byte chunk.
   */
  public static String token(int numBytes) {
    byte[] bytes = new byte[numBytes];
    secureRandom.nextBytes(bytes);

    return toHex(bytes);
  }

  /**
   * Get a hex string representation of an array of bytes.
   *
   * @param bytes The bytes to reflect
   *
   * @return The hex representation of the input bytes.
   */
  public static String toHex(byte[] bytes) {
    char[] chars = new char[bytes.length * 2];
    for (var i = 0; i < bytes.length; i++) {
      int odx = bytes[i] & 0xFF;
      chars[i * 2] = HEX[odx >>> 4];
      chars[i * 2 + 1] = HEX[odx & 0x0F];
    }

    return new String(chars);
  }

  /**
   * Create a SHA256 digest of the given input string and return the hex representation.
   *
   * @param str The input to digest.
   *
   * @return THe hex representatino of the SHA256 digest.
   */
  @SneakyThrows
  public static String sha256(String str) {
    var digest = MessageDigest.getInstance("SHA256");
    digest.update(str.getBytes(StandardCharsets.UTF_8));
    return toHex(digest.digest());
  }

  /**
   * Create an MD5 digest of the given input string and return the hex representation.
   *
   * @param str The input to digest
   *
   * @return The hex representation of the MD5 digest.
   */
  @SneakyThrows
  public static String md5(String str) {
    var digest = MessageDigest.getInstance("MD5");
    digest.update(str.getBytes(StandardCharsets.UTF_8));
    return toHex(digest.digest());
  }
}
