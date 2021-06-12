package com.mtinge.EBMLReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.nio.ByteBuffer;

/*
  EBML spec is located at http://matroska.sourceforge.net/technical/specs/index.html

  Example element:
    1A 45 DF A3 01 00 00 00 00 00 00 23

    HEADER:
      1A 45 DF A3
      first byte: 1A // 00011010
      leading zeros: 3
      ID length (including size descriptor): 4 bytes (3 leading zeros + 1)

    DATA_SIZE:
      01
      first byte: 01
      leading zeros: 7
      data length (including size descriptor): 8 bytes (7 leading zeros + 1)

    DATA:
      00 00 00 00 00 00 23
 */

/**
 * A reader to identify the type of file this EBML container is.
 *
 * @author MidnightTinge
 */
public class EBMLReader {
  private static final Logger logger = LoggerFactory.getLogger(EBMLReader.class);
  private final PushbackInputStream buffer;

  private EBMLReader(byte[] chunk) throws IOException {
    this.buffer = new PushbackInputStream(new ByteArrayInputStream(chunk));

    // verify EBML header
    var header = readField();
    if (header == null || header.length != 4) {
      throw new IllegalArgumentException("Invalid buffer: Not enough bytes to validate header.");
    }

    int[] expectedHeader = new int[]{0x1A, 0x45, 0xDF, 0xA3};
    for (int i = 0; i < expectedHeader.length; i++) {
      if ((header[i] & 0xFF) != expectedHeader[i]) {
        throw new IllegalArgumentException("Invalid buffer: Invalid EBML header.");
      }
    }

    // Discard the EBML characteristics payload, we don't use it
    readField();
  }


  /**
   * Reads an EBML field.<br /> EBML fields start with a size-encoding byte which denotes the length
   * of the data, and fields make up everything in an EBML document.
   *
   * @return The field
   *
   * @throws IOException
   */
  private byte[] readField() throws IOException {
    byte[] _first = buffer.readNBytes(1);
    if (_first.length != 1) {
      return null;
    }
    buffer.unread(_first);

    var firstByte = _first[0];
    var mask = 0x80;
    var intClass = 0;

    while ((firstByte & mask) == 0) {
      ++intClass;
      mask >>= 1;
    }

    var id = new byte[intClass + 1];
    buffer.readNBytes(id, 0, id.length);

    return id;
  }

  /**
   * Consumes the next EBML element.
   *
   * @return The next EBMLElement, or null if we've run out of data.
   *
   * @throws IOException
   */
  private EBMLElement nextElement() throws IOException {
    var id = readField();
    var lenField = readField();
    if (id == null || lenField == null) return null;

    lenField[0] ^= 0x80 >> (lenField.length - 1);

    long len = pack(lenField);
    var data = buffer.readNBytes(Long.valueOf(len).intValue());

    return new EBMLElement(id, pack(lenField), data);
  }

  /**
   * Checks if the given chunk of data matches the expected ints. Input bytes will be coerced to
   * ints.
   *
   * @param needle The chunk of data to validate. Will be coerced to ints (needle[N] & 0xFF).
   * @param haystack The valid chunk of data to test against.
   *
   * @return Whether or not {@code needle} matches {@code haystack}
   */
  private boolean matches(byte[] needle, int[] haystack) {
    if (needle.length != haystack.length) return false;

    for (var i = 0; i < needle.length; i++) {
      if ((needle[i] & 0xFF) != haystack[i]) {
        return false;
      }
    }

    return true;
  }

  /**
   * Checks if the given chunk of data matches the expected ints.
   *
   * @param needle The chunk of data to validate.
   * @param haystack The valid chunk of data to test against.
   *
   * @return Whether or not {@code needle} matches {@code haystack}
   */
  private boolean matches(byte[] needle, byte[] haystack) {
    if (needle.length != haystack.length) return false;

    for (var i = 0; i < needle.length; i++) {
      if (needle[i] != haystack[i]) {
        return false;
      }
    }

    return true;
  }

  /**
   * Packs a set of bytes into a short, int, or long depending on the number provided.<br /> Uses a
   * {@link ByteBuffer}
   *
   * @param bytes The bytes to pack
   *
   * @return The packed number
   */
  private long pack(byte[] bytes) {
    if (bytes.length == 0) return 0;
    if (bytes.length == 1) return bytes[0] & 0xFF;

    var bb = ByteBuffer.wrap(bytes);
    if (bytes.length == 2) {
      return bb.getShort();
    } else if (bytes.length == 4) {
      return bb.getInt();
    } else if (bytes.length == 8) {
      return bb.getLong();
    }

    return 0;
  }

  /**
   * Read a file with an EBML container and extract the type of video that is embedded (WebM/MKV)
   *
   * @param chunk A chunk of the beginning of the file. Must contain a valid EBML header.
   *
   * @return The mime-type of the embedded video.
   *
   * @throws IllegalArgumentException A general exception can be thrown if the file is not a valid
   *   EBML container.
   * @throws IOException An exception can be thrown by the underlying {@link PushbackInputStream}
   *   when peeking at bytes.
   */
  public static String identify(byte[] chunk) throws Exception {
    try {
      var reader = new EBMLReader(chunk);

      EBMLElement elem;
      while ((elem = reader.nextElement()) != null) {
        if (reader.matches(elem.header, new int[]{0x42, 0x82})) {
          var docType = new String(elem.data);
          switch (docType.toLowerCase().trim()) {
            case "webm" -> {
              return "video/webm";
            }
            case "matroska" -> {
              return "video/x-matroska";
            }
          }
          break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return null;
  }

  public static final class EBMLElement {
    public final byte[] header;
    public final long length;
    public final byte[] data;

    public EBMLElement(byte[] header, long length, byte[] data) {
      this.header = header;
      this.length = length;
      this.data = data;
    }
  }
}
