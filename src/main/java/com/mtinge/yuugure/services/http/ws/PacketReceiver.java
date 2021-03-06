package com.mtinge.yuugure.services.http.ws;

import com.mtinge.yuugure.core.MoshiFactory;
import com.mtinge.yuugure.core.PrometheusMetrics;
import com.mtinge.yuugure.services.http.ws.packets.BinaryPacket;
import com.mtinge.yuugure.services.http.ws.packets.PacketFactory;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.Moshi;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import okio.Okio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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
    PrometheusMetrics.WS_PACKETS_TOTAL.inc();
    var reader = JsonReader.of(Okio.buffer(Okio.source(new ByteArrayInputStream(message.getData().getBytes(StandardCharsets.UTF_8)))));
    reader.setLenient(true);

    if (reader.peek() != JsonReader.Token.BEGIN_OBJECT) {
      if (channel.isOpen()) {
        WebSockets.sendBinary(ByteBuffer.wrap(BinaryPacket.REJECTED_INVALID_DATA().data), channel, null);
      }
      logger.debug("Invalid packet received, reader's head token is not BEGIN_OBJECT, instead received '{}'.", reader.peek());
      return;
    }

    boolean handled = false;

    try {
      var deserialized = (Map<String, Object>) moshi.adapter(Object.class).fromJson(reader);
      if (deserialized != null) {
        var type = (String) deserialized.getOrDefault("type", "");

        if (type != null && !type.isBlank()) {
          PrometheusMetrics.WS_PACKETS_TYPED_TOTAL.labels(type.toLowerCase().trim()).inc();
          var packet = this.factory.getByType(type);
          if (packet != null) {
            handled = true;
            try {
              packet.handleAction(socket, deserialized);
              PrometheusMetrics.WS_PACKETS_HANDLED_TOTAL.labels(type).inc();
            } catch (Exception e) {
              PrometheusMetrics.WS_PACKETS_ERRORED_TOTAL.labels(type).inc();
              logger.error("The handler for packet {} threw an error.", packet.type, e);
            }
          } else {
            if (channel.isOpen()) {
              WebSockets.sendBinary(ByteBuffer.wrap(BinaryPacket.REJECTED_UNKNOWN_TYPE(type).data), channel, null);
              handled = true;
            }
            PrometheusMetrics.WS_PACKETS_UNHANDLED_TOTAL.labels(type.toLowerCase().trim()).inc();
            logger.warn("Unhandled WebSocket packet: {}.", type);
          }
        } else {
          PrometheusMetrics.WS_PACKETS_INVALID_TOTAL.inc();
          logger.debug("Discarded invalid packet, type was not present.");
        }
      } else {
        PrometheusMetrics.WS_PACKETS_INVALID_TOTAL.inc();
        logger.debug("Discarded invalid packet, deserialized was null.");
      }
    } catch (Exception e) {
      PrometheusMetrics.WS_PACKETS_INVALID_TOTAL.inc();
      logger.error("Failed to handle packet (outer).", e);
    } finally {
      if (!handled) {
        if (channel.isOpen()) {
          WebSockets.sendBinary(ByteBuffer.wrap(BinaryPacket.REJECTED_INVALID_DATA().data), channel, null);
        }
      }
    }
  }
}
