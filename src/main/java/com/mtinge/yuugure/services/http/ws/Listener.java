package com.mtinge.yuugure.services.http.ws;

import com.mtinge.yuugure.core.PrometheusMetrics;
import com.mtinge.yuugure.services.http.handlers.QueryHandler;
import com.mtinge.yuugure.services.http.handlers.SessionHandler;
import com.mtinge.yuugure.services.http.ws.packets.Packet;
import com.mtinge.yuugure.services.http.ws.packets.PacketFactory;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import lombok.AccessLevel;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Getter
public class Listener {
  private static final Logger logger = LoggerFactory.getLogger(Listener.class);

  @Getter(AccessLevel.NONE)
  private final Object _conMonitor = new Object();

  private LinkedList<WrappedSocket> sockets = new LinkedList<>();
  private ConcurrentHashMap<Integer, LinkedList<WrappedSocket>> accountConnections = new ConcurrentHashMap<>();

  private final Lobby lobby = new Lobby();
  private final PacketFactory packetFactory;
  private final Pattern validSubscribePattern = Pattern.compile("^(?:upload|account):[0-9]+$", Pattern.CASE_INSENSITIVE);
  private final AtomicInteger connections = new AtomicInteger(0);
  private final AtomicInteger authedConnections = new AtomicInteger(0);

  public Listener() {
    this.packetFactory = new PacketFactory();
    packetFactory.register(
      Packet.builder("sub")
        .handleAction((socket, map) -> {
          var room = map.getOrDefault("room", "");
          if (room instanceof String) {
            var toSub = ((String) room).toLowerCase().trim();
            if (!toSub.isBlank()) {
              if (validSubscribePattern.matcher(toSub).find()) {
                lobby.in(toSub).join(socket);
              }
            }
          }
        })
        .build(),
      Packet.builder("unsub")
        .handleAction((socket, map) -> {
          var room = map.getOrDefault("room", "");
          if (room instanceof String) {
            var toSub = ((String) room).toLowerCase().trim();
            if (!toSub.isBlank()) {
              lobby.in(toSub).leave(socket);
            }
          }
        })
        .build()
    );
  }

  public void newConnection(WebSocketHttpExchange exchange, WebSocketChannel channel) {
    PrometheusMetrics.WS_CONNECTIONS_TOTAL.inc();

    var query = exchange.getAttachment(QueryHandler.ATTACHMENT_KEY);
    var authed = exchange.getAttachment(SessionHandler.ATTACHMENT_KEY);

    // Attach our account ID and create a WrappedSocket
    WrappedSocket socket;
    if (authed != null) {
      PrometheusMetrics.WS_CONNECTIONS_AUTHED_TOTAL.inc();
      socket = new WrappedSocket(channel, authed.id);
      synchronized (_conMonitor) {
        var cons = accountConnections.compute(authed.id, (k, v) -> {
          if (v == null) {
            v = new LinkedList<>();
          }

          v.add(socket);
          return v;
        });
        if (cons.size() == 1) {
          PrometheusMetrics.WS_CONNECTIONS_AUTHED_CURRENT.set(authedConnections.incrementAndGet());
        }
      }
    } else {
      PrometheusMetrics.WS_CONNECTIONS_UNAUTHED_TOTAL.inc();
      socket = new WrappedSocket(channel);
    }

    // Pass the socket off to the connection handler
    handleSocket(socket, query != null ? query.getFirst("intent") : null);
  }

  private void handleSocket(WrappedSocket socket, String intents) {
    PrometheusMetrics.WS_CONNECTIONS_TOTAL_CURRENT.set(connections.incrementAndGet());
    synchronized (_conMonitor) {
      sockets.add(socket);
    }

    socket.channel().getReceiveSetter().set(new PacketReceiver(socket, packetFactory));
    socket.channel().getCloseSetter().set(c -> {
      synchronized (_conMonitor) {
        PrometheusMetrics.WS_CONNECTIONS_TOTAL_CURRENT.set(connections.decrementAndGet());

        var iter = sockets.iterator();
        while (iter.hasNext()) {
          var iterSock = iter.next();
          if (iterSock.channel().equals(c)) {
            // Unlink from the sockets list
            iter.remove();

            // Clean up account connections if necessary
            if (iterSock.accountId() != null) {
              accountConnections.computeIfPresent(socket.accountId(), (k, v) -> {
                v.remove(iterSock.channel()); // note: hashCode/equals has been extended in WrappedSocket to handle this.
                return v;
              });
            }

            // Unsubscribe from rooms
            lobby.leaving(iterSock);
            break;
          }
        }

        if (socket.accountId() != null) {
          var cons = this.accountConnections.get(socket.accountId());
          if (cons != null && cons.isEmpty()) {
            PrometheusMetrics.WS_CONNECTIONS_AUTHED_CURRENT.set(authedConnections.decrementAndGet());
          }
        }
      }
    });

    var intent = Objects.requireNonNullElse(intents, "all").toLowerCase().trim();
    if (!intent.equalsIgnoreCase("all")) {
      // nothing to do at this stage.
    } else {
      // TODO check permissions of user and add to moderation rooms
    }

    socket.channel().resumeReceives();
  }
}
