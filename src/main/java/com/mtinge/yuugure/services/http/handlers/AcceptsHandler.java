package com.mtinge.yuugure.services.http.handlers;

import com.mtinge.AcceptParser.Parser;
import com.mtinge.AcceptParser.ParserResult;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;

public class AcceptsHandler implements HttpHandler {
  public static AttachmentKey<ParserResult> ATTACHMENT_KEY = AttachmentKey.create(ParserResult.class);
  private HttpHandler next;

  public AcceptsHandler(HttpHandler next) {
    this.next = next;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    var header = exchange.getRequestHeaders().contains(Headers.ACCEPT) ? exchange.getRequestHeaders().getFirst(Headers.ACCEPT) : "*/*";
    exchange.putAttachment(ATTACHMENT_KEY, Parser.parse(header));

    next.handleRequest(exchange);
  }
}
