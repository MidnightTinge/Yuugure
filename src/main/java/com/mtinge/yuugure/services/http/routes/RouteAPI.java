package com.mtinge.yuugure.services.http.routes;

import com.mtinge.yuugure.services.http.api.AccountResource;
import com.mtinge.yuugure.services.http.api.CommentResource;
import com.mtinge.yuugure.services.http.api.ProfileResource;
import com.mtinge.yuugure.services.http.api.UploadResource;
import io.undertow.Handlers;
import io.undertow.server.handlers.PathHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteAPI extends Route {
  private static final Logger logger = LoggerFactory.getLogger(RouteAPI.class);

  private final PathHandler pathHandler;

  public RouteAPI() {
    super();
    this.pathHandler = Handlers.path()
      .addPrefixPath("/upload", new UploadResource().getRoutes())
      .addPrefixPath("/account", new AccountResource().getRoutes())
      .addPrefixPath("/profile", new ProfileResource().getRoutes())
      .addPrefixPath("/comment", new CommentResource().getRoutes());
  }

  @Override
  public PathHandler wrap(PathHandler chain) {
    return chain.addPrefixPath("/api", pathHandler);
  }
}
