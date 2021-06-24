package com.mtinge.yuugure.services.http.ws;

import com.mtinge.yuugure.core.MoshiFactory;
import com.mtinge.yuugure.services.http.ws.packets.OutgoingPacket;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.Moshi;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Accessors(fluent = true)
public class WrappedSocket {
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
