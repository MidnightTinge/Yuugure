package com.mtinge.yuugure.services.http.ws.packets;

import com.mtinge.yuugure.services.http.ws.WrappedSocket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * An incoming WebSocket packet descriptor and associated action handler.
 *
 * @see PacketFactory
 */
@AllArgsConstructor
public class Packet {
  public final String type;
  private final BiConsumer<WrappedSocket, Map<String, Object>> handleAction;

  public void handleAction(WrappedSocket socket, Map<String, Object> map) {
    if (handleAction != null) {
      handleAction.accept(socket, map);
    }
  }

  public static Builder builder(String type) {
    return new Builder(type);
  }

  @Getter
  @Setter
  @Accessors(fluent = true)
  public static final class Builder {
    private String type;
    private BiConsumer<WrappedSocket, Map<String, Object>> handleAction;

    public Builder(String type) {
      this.type = type;
    }

    public Packet build() {
      type = Objects.requireNonNull(type);

      return new Packet(type, handleAction);
    }
  }
}
