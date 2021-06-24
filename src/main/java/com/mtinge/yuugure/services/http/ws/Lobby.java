package com.mtinge.yuugure.services.http.ws;

import java.util.HashMap;

/**
 * A WebSocket room manager.
 */
public class Lobby {
  private final Object _monitor = new Object();
  private final HashMap<String, Room> rooms = new HashMap<>();

  public Room get(String roomName) {
    synchronized (_monitor) {
      return rooms.compute(roomName, (k, v) -> {
        if (v == null) {
          v = new Room(k);
        }

        return v;
      });
    }
  }

  public Room in(String roomName) {
    return get(roomName);
  }

  public void closeRoom(String roomName) {
    synchronized (_monitor) {
      if (rooms.containsKey(roomName)) {
        rooms.get(roomName).close();
        rooms.remove(roomName);
      }
    }
  }

  public void leaving(WrappedSocket socket) {
    synchronized (_monitor) {
      for (var rn : socket.rooms()) {
        get(rn).leave(socket);
      }
    }
  }

}
