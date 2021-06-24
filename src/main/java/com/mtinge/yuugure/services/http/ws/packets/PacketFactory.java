package com.mtinge.yuugure.services.http.ws.packets;

import java.util.HashMap;
import java.util.HashSet;

public class PacketFactory {
  private final Object _monitor = new Object();
  private final HashMap<String, Packet> typePacketMap = new HashMap<>();
  private final HashSet<Packet> packets = new HashSet<>();

  public PacketFactory register(Packet... toRegister) {
    synchronized (_monitor) {
      for (var packet : toRegister) {
        if (packets.add(packet)) {
          typePacketMap.put(packet.type, packet);
        }
      }
    }

    return this;
  }

  public boolean remove(Packet packet) {
    return remove(packet.type);
  }

  public boolean remove(String type) {
    synchronized (_monitor) {
      var packet = getByType(type);
      if (packet != null && packets.remove(packet)) {
        typePacketMap.remove(type);
        return true;
      }
    }

    return false;
  }

  public Packet getByType(String type) {
    return typePacketMap.get(type);
  }

}
