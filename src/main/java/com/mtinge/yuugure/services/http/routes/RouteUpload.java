package com.mtinge.yuugure.services.http.routes;

import com.mtinge.yuugure.services.http.Responder;
import com.mtinge.yuugure.services.http.handlers.RequireAuthHandler;
import com.mtinge.yuugure.services.http.handlers.ViewHandler;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.util.PathTemplateMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteUpload extends Route {
  private static final Logger logger = LoggerFactory.getLogger(RouteUpload.class);
  private static RouteUpload instance;
  private static final Object _lock = new Object();

  private PathHandler pathHandler;

  public RouteUpload() {
    this.pathHandler = Handlers.path().addExactPath("/", new ViewHandler("app"));
  }

  public PathHandler wrap(PathHandler chain) {
    return chain.addPrefixPath("/upload", pathHandler);
  }
}
