package com.mtinge.yuugure.services.http.ws.packets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PacketFactoryTest {
  @Test
  @DisplayName("Registers multiple packets")
  void registersMultiplePackets() {
    var factory = new PacketFactory();
    factory.register(
      Packet.builder("type1").build(),
      Packet.builder("type2").build()
    );

    Assertions.assertNotNull(factory.getByType("type1"));
    Assertions.assertNotNull(factory.getByType("type2"));
  }

  @Test
  @DisplayName("Remove returns true for existing type")
  void removeReturnsTrueForExistingType() {
    var factory = new PacketFactory()
      .register(
        Packet.builder("exists").build()
      );
    Assertions.assertTrue(factory.remove("exists"));
  }

  @Test
  @DisplayName("Remove returns false for non-existant type")
  void removeReturnsFalseForNonExistantType() {
    var factory = new PacketFactory();
    Assertions.assertFalse(factory.remove("nonexistent"));
  }

  @Test
  @DisplayName("Removes a specific packet by type")
  void removesSpecificPacket() {
    var factory = new PacketFactory()
      .register(
        Packet.builder("type1").build(),
        Packet.builder("type2").build()
      );

    Assertions.assertNotNull(factory.getByType("type1"));
    Assertions.assertTrue(factory.remove("type1"));
    Assertions.assertNull(factory.getByType("type1"));
  }

  @Test
  @DisplayName("Removes a requested packet")
  void removesRequestedPacket() {
    var packet1 = Packet.builder("type1").build();
    var packet2 = Packet.builder("type2").build();
    var factory = new PacketFactory()
      .register(packet1, packet2);

    Assertions.assertNotNull(factory.getByType(packet1.type));
    Assertions.assertTrue(factory.remove(packet1));
    Assertions.assertNull(factory.getByType(packet1.type));
  }
}
