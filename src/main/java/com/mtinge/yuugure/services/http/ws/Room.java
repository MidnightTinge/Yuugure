package com.mtinge.yuugure.services.http.ws;

import com.mtinge.yuugure.core.MoshiFactory;
import com.squareup.moshi.Moshi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * A WebSocket room that is used to scope messages and connections.
 */
public class Room {
  private final Object _monitor = new Object();
  private final Moshi moshi;
  private final Logger logger;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final HashSet<WrappedSocket> sockets;
  public final String name;

  public Room(String name) {
    this.logger = LoggerFactory.getLogger("room-" + name);
    this.moshi = MoshiFactory.create();
    this.sockets = new HashSet<>();
    this.name = name;
  }

  public boolean join(WrappedSocket socket) {
    if (closed.get()) {
      return false;
    }

    synchronized (_monitor) {
      socket.rooms().add(this.name);
      return sockets.add(socket);
    }
  }

  public boolean leave(WrappedSocket socket) {
    if (closed.get()) {
      return false;
    }

    synchronized (_monitor) {
      socket.rooms().remove(this.name);
      return sockets.remove(socket);
    }
  }

  public void broadcast(Object packet) {
    if (closed.get()) {
      return;
    }

    var send = WrappedSocket.packPacket(packet);
    synchronized (_monitor) {
      for (var socket : sockets) {
        socket.send(send);
      }
    }
  }

  public void broadcast(Object packet, Predicate<WrappedSocket> predicate) {
    if (closed.get()) {
      return;
    }

    var send = WrappedSocket.packPacket(packet);
    synchronized (_monitor) {
      for (var socket : sockets) {
        if (predicate.test(socket)) {
          socket.send(send);
        }
      }
    }
  }

  public void close() {
    if (closed.compareAndSet(false, true)) {
      synchronized (_monitor) {
        for (var socket : sockets) {
          socket.rooms().remove(this.name);
        }

        sockets.clear();
      }
    }
  }

  private String preparePacket(Object packet) {
    if (!(packet instanceof String)) {
      packet = moshi.adapter(Object.class).toJson(packet);
    }

    return (String) packet;
  }

  public String toString() {
    return "Room(name=" + name + ", closed=" + closed.get() + ")";
  }
}
