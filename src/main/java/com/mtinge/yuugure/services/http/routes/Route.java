package com.mtinge.yuugure.services.http.routes;

import com.mtinge.yuugure.services.http.util.MethodValidator;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;

public abstract class Route {
  /**
   * Checks if the request conforms to the provided HTTP methods.
   *
   * @param exchange The exchange to validate
   * @param methods The {@link Methods} which are acceptable
   *
   * @return True if the request method for this exchange is valid. If the request is not
   *   acceptable, then the request is terminated and the MethodNotAllowed header is attached to the
   *   response.
   *
   * @see Methods
   */
  protected boolean validateMethods(HttpServerExchange exchange, HttpString... methods) {
    return MethodValidator.handleMethodValidation(exchange, methods);
  }

  public abstract PathHandler wrap(PathHandler chain);
}
