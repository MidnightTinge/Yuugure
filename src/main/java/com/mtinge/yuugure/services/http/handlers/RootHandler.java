package com.mtinge.yuugure.services.http.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class RootHandler implements HttpHandler {
  private final HttpHandler next;

  public RootHandler(HttpHandler next) {
    this.next = next;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    next.handleRequest(exchange);
  }
}
