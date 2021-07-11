package com.mtinge.yuugure.services.http.ws.packets;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * <p>A binary WebSocket packet spec assembler. The format is purposefully overly simplistic:</p>
 * <pre>
 *   [
 *     PACKET_TYPE,
 *     PACKET_HEADER...,
 *     PAYLOAD_SEPARATOR,
 *     PAYLOAD...
 *   ]
 * </pre>
 */
public class BinaryPacket {
  public static byte PAYLOAD_SEPARATOR = 0;

  public static byte TYPE_ACK = 1;
  public static byte TYPE_REJECTED = 2;

  public static byte HEADER_ACK_SUB = 1;
  public static byte HEADER_ACK_UNSUB = 2;

  public static byte HEADER_REJECTED_UNKNOWN_TYPE = 1;
  public static byte HEADER_REJECTED_INVALID_DATA = 2;

  public final byte[] data;

  /**
   * Create a BinaryPacket that only consists of a type.
   *
   * @param type The type of packet.
   */
  public BinaryPacket(byte type) {
    this.data = new byte[]{type, 0};
  }

  /**
   * Create a BinaryPacket that has a header with a single byte. Convenience method for {@link
   * #BinaryPacket(byte, byte[], byte[])}
   *
   * @param type The type of packet.
   * @param header The header byte.
   * @param data The payload.
   */
  public BinaryPacket(byte type, byte header, byte[] data) {
    this(type, new byte[]{header}, data);
  }

  /**
   * Creates a BinaryPacket.
   *
   * @param type The type of packet.
   * @param header The header bytes.
   * @param data The payload.
   */
  public BinaryPacket(byte type, byte[] header, byte[] data) {
    // type + header + separator + payload
    var bb = ByteBuffer.allocate(1 + header.length + 1 + data.length);

    bb.put(type);
    bb.put(header);
    bb.put(PAYLOAD_SEPARATOR);
    bb.put(data);

    this.data = bb.array();
  }

  /**
   * Create a new ACK_SUB packet.
   *
   * @param room The room that is being acknowledged as subscribed.
   *
   * @return The {@link BinaryPacket}
   */
  public static BinaryPacket ACK_SUB(String room) {
    return new BinaryPacket(TYPE_ACK, HEADER_ACK_SUB, room.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Create a new ACK_UNSUB packet.
   *
   * @param room The room that is being acknowledged as unsubsubscribed.
   *
   * @return The {@link BinaryPacket}
   */
  public static BinaryPacket ACK_UNSUB(String room) {
    return new BinaryPacket(TYPE_ACK, HEADER_ACK_UNSUB, room.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Creates the UNKNOWN_TYPE response packet that is sent on incoming packets with an unhandled
   * TYPE directive.
   *
   * @param type The unhandled type directive.
   *
   * @return The {@link BinaryPacket}
   */
  public static BinaryPacket REJECTED_UNKNOWN_TYPE(String type) {
    return new BinaryPacket(TYPE_REJECTED, HEADER_REJECTED_UNKNOWN_TYPE, type.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Creates the REJECTED_INVALID_DATA packet that is sent on invalid incoming packets (e.g. json
   * wouldn't parse, or missing a required field for processing)
   *
   * @return The {@link BinaryPacket}
   */
  public static BinaryPacket REJECTED_INVALID_DATA() {
    return new BinaryPacket(TYPE_REJECTED, HEADER_REJECTED_INVALID_DATA, new byte[]{});
  }
}
