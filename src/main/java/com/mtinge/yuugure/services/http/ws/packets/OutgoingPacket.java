package com.mtinge.yuugure.services.http.ws.packets;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class OutgoingPacket {
  private final Map<String, Object> data;

  private OutgoingPacket() {
    this.data = new LinkedHashMap<>();
  }

  public OutgoingPacket addData(String key, Object value) {
    data.put(key, value);
    return this;
  }

  public static OutgoingPacket prepare(String type) {
    return new OutgoingPacket().addData("type", type);
  }

}
