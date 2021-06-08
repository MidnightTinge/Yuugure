package com.mtinge.yuugure.services.http.handlers;

import com.mtinge.yuugure.App;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class AddressHandler implements HttpHandler {
  public static final AttachmentKey<InetAddress> ATTACHMENT_KEY = AttachmentKey.create(InetAddress.class);
  private static final Logger logger = LoggerFactory.getLogger(AddressHandler.class);

  private final HttpHandler next;

  public AddressHandler(HttpHandler next) {
    this.next = next;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    try {
      var header = App.config().http.realIPHeader;
      if (header != null) {
        if (exchange.getRequestHeaders().contains(header)) {
          var fromHeader = exchange.getRequestHeaders().get(header).getFirst();
          if (fromHeader.isBlank()) {
            exchange.endExchange();
            logger.warn("Request to \"{}\" rejected. The required header {} was blank.", exchange.getRequestURL(), header);
          } else {
            exchange.putAttachment(ATTACHMENT_KEY, InetAddress.getByName(fromHeader)); // a fully qualified IP will not trigger a DNS query
          }
        } else {
          exchange.endExchange();
          logger.warn("Request to \"{}\" rejected. Missing required {} header.", exchange.getRequestURL(), header);
        }
      } else {
        exchange.putAttachment(ATTACHMENT_KEY, exchange.getSourceAddress().getAddress());
      }
    } catch (Exception e) {
      logger.error("Failed to handle request", e);
    }

    if (!exchange.isComplete()) {
      this.next.handleRequest(exchange);
    }
  }
}
