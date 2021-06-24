package com.mtinge.yuugure.services.http.ws;

import com.mtinge.yuugure.core.MoshiFactory;
import com.mtinge.yuugure.services.http.ws.packets.PacketFactory;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.Moshi;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import okio.Okio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class PacketReceiver extends AbstractReceiveListener {
  private static final Logger logger = LoggerFactory.getLogger(PacketReceiver.class);
  private final Moshi moshi = MoshiFactory.create();
  private final WrappedSocket socket;
  private final PacketFactory factory;

  public PacketReceiver(WrappedSocket socket, PacketFactory factory) {
    this.socket = socket;
    this.factory = factory;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
    var reader = JsonReader.of(Okio.buffer(Okio.source(new ByteArrayInputStream(message.getData().getBytes(StandardCharsets.UTF_8)))));
    reader.setLenient(true);

    if (reader.peek() != JsonReader.Token.BEGIN_OBJECT) {
      logger.debug("Invalid packet received, reader's head token is not BEGIN_OBJECT, instead received '{}'.", reader.peek());
      return;
    }

    try {
      var deserialized = (Map<String, Object>) moshi.adapter(Object.class).fromJson(reader);
      if (deserialized != null) {
        var type = (String) deserialized.getOrDefault("type", "");

        // TODO expose ws_packet counters in prometheus
        if (type != null && !type.isBlank()) {
          var packet = this.factory.getByType(type);
          if (packet != null) {
            try {
              packet.handleAction(socket, deserialized);
            } catch (Exception e) {
              logger.error("The handler for packet {} threw an error.", packet.type, e);
            }
          } else {
            logger.warn("Unhandled WebSocket packet: {}.", type);
          }
        } else {
          logger.debug("Discarded invalid packet, type was not present.");
        }
      } else {
        logger.debug("Discarded invalid packet, deserialized was null.");
      }
    } catch (Exception e) {
      logger.error("Failed to handle packet (outer).", e);
    }
  }
}
