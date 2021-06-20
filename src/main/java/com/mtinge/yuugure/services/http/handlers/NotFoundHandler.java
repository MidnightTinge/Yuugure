package com.mtinge.yuugure.services.http.handlers;

import com.mtinge.yuugure.services.http.Responder;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class NotFoundHandler implements HttpHandler {
  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    Responder.with(exchange).notFound();
  }
}
