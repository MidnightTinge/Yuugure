package com.mtinge.yuugure.services.http.handlers;

import com.mtinge.yuugure.core.PrometheusMetrics;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class RootHandler implements HttpHandler {
  private final HttpHandler next;

  public RootHandler(HttpHandler next) {
    this.next = next;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    var timer = PrometheusMetrics.HTTP_REQUEST_TIMING.labels(exchange.getRequestMethod().toString()).startTimer();
    exchange.addExchangeCompleteListener((exchange1, nextListener) -> {
      timer.observeDuration();
      nextListener.proceed();
    });

    PrometheusMetrics.HTTP_REQUESTS_TOTAL.labels(exchange.getRequestMethod().toString()).inc();
    next.handleRequest(exchange);
  }
}
