package com.mtinge.yuugure.services.http.handlers;

import com.mtinge.yuugure.services.http.Responder;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class RequireAuthHandler implements HttpHandler {
  private final HttpHandler next;

  public RequireAuthHandler(HttpHandler next) {
    this.next = next;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    if (exchange.getAttachment(SessionHandler.ATTACHMENT_KEY) == null) {
      Responder.with(exchange).notAuthorized();
    } else {
      next.handleRequest(exchange);
    }
  }
}
