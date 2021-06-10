package com.mtinge.yuugure.services.http.routes;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.MoshiFactory;
import com.mtinge.yuugure.services.http.Responder;
import com.squareup.moshi.Moshi;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteIndex extends Route {
  private static final Logger logger = LoggerFactory.getLogger(RouteIndex.class);
  private static RouteIndex instance;
  private static final Object _lock = new Object();

  private PathHandler pathHandler;
  private Moshi moshi;

  public RouteIndex() {
    this.moshi = MoshiFactory.create();
    this.pathHandler = Handlers.path()
      .addExactPath("/", this::index)
      .addPrefixPath("/", App.webServer().staticHandler());
  }

  @Override
  public PathHandler wrap(PathHandler chain) {
    return chain.addPrefixPath("/", pathHandler);
  }

  private void index(HttpServerExchange exchange) {
    Responder.with(exchange).view("app");
  }
}
