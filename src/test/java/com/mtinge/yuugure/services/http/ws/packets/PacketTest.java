package com.mtinge.yuugure.services.http.ws.packets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class PacketTest {
  @Test
  @DisplayName("Builder throws if type is null")
  void builderThrowsForNullType() {
    Assertions.assertThrows(Throwable.class, () -> {
      Packet.builder("").type(null).build();
    });
  }

  @Test
  @DisplayName("handleAction does not throw for a null handler")
  void handleActionDoesNotThrowOnNull() {
    var packet = Packet.builder("type1").build();
    Assertions.assertDoesNotThrow(() -> {
      packet.handleAction(null, null);
    });
  }

  @Test
  @DisplayName("handleAction actually fires")
  void handleActionFires() {
    AtomicInteger i = new AtomicInteger(0);
    var packet = Packet.builder("type1")
      .handleAction((socket, map) -> {
        i.incrementAndGet();
      }).build();
    packet.handleAction(null, null);
    Assertions.assertEquals(i.get(), 1);
  }
}
