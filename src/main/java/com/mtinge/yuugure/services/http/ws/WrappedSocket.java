package com.mtinge.yuugure.services.http.ws;

import com.mtinge.yuugure.core.MoshiFactory;
import com.mtinge.yuugure.services.http.ws.packets.BinaryPacket;
import com.mtinge.yuugure.services.http.ws.packets.OutgoingPacket;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.Moshi;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Accessors(fluent = true)
public class WrappedSocket {
  private static final Logger logger = LoggerFactory.getLogger(WrappedSocket.class);

  private static final Moshi moshi = MoshiFactory.create();
  private static final AtomicLong idIncrementer = new AtomicLong(0);

  private HashSet<String> rooms;
  private WebSocketChannel channel;
  private Integer accountId;
  private Long socketId;

  public WrappedSocket(WebSocketChannel channel) {
    this(channel, null);
  }

  public WrappedSocket(WebSocketChannel channel, Integer accountId) {
    this.channel = channel;
    this.accountId = accountId;
    this.socketId = idIncrementer.incrementAndGet();
    this.rooms = new HashSet<>();
  }

  public void send(Object packet) {
    if (channel.isOpen()) {
      WebSockets.sendText(packPacket(packet), channel, null);
    } else {
      logger.warn("Attempted to send to a closed WebSocketChannel. Packet discarded.");
    }
  }

  public void sendBinary(BinaryPacket packet) {
    sendBinary(packet.data);
  }

  public void sendBinary(byte[] bin) {
    if (channel.isOpen()) {
      WebSockets.sendBinary(ByteBuffer.wrap(bin), channel, null);
    } else {
      logger.warn("Attempted to send to a closed WebSocketChannel. Packet discarded.");
    }
  }

  public static String packPacket(Object packet) {
    if (!(packet instanceof String)) {
      try {
        if (packet instanceof OutgoingPacket) {
          packet = moshi.adapter(Object.class).toJson(((OutgoingPacket) packet).getData());
        } else {
          packet = moshi.adapter(Object.class).toJson(packet);
        }
      } catch (JsonDataException jde) {
        throw new IllegalArgumentException("Invalid packet supplied: failed to convert to JSON.", jde);
      }
    }

    return (String) packet;
  }

  public int hashCode() {
    return channel.hashCode();
  }

  public boolean equals(Object other) {
    if (other instanceof WebSocketChannel) {
      return channel.equals(other);
    } else if (other instanceof WrappedSocket) {
      return channel.equals(((WrappedSocket) other).channel);
    }

    return false;
  }

}
