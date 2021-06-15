package com.mtinge.yuugure.services.http.handlers;

import com.mtinge.yuugure.services.http.Responder;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;

public class RootHandler implements HttpHandler {
  private final HttpHandler next;

  public RootHandler(HttpHandler next) {
    this.next = next;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    // Add a default "not found" handler to render the app, enabling Single-Page Application mode.
    exchange.addDefaultResponseListener(xch -> {
      if (!xch.isComplete() && !xch.isResponseStarted() && xch.isResponseChannelAvailable()) {
        if (xch.getStatusCode() == 404 && xch.getRequestMethod().equals(Methods.GET)) {
          var res = Responder.with(xch);
          if (!res.wantsJson()) {
            res.status(200).view("app");
            return true;
          }
        }
      }
      return false;
    });

    next.handleRequest(exchange);
  }
}
