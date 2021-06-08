package com.mtinge.yuugure.services.http.routes;

import com.mtinge.yuugure.services.http.Responder;
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
    boolean found = false;

    for (HttpString method : methods) {
      if (exchange.getRequestMethod().equals(method)) {
        found = true;
        break;
      }
    }

    if (!found) {
      Responder.with(exchange).methodNotAllowed(methods);
    }
    return found;
  }

  public abstract PathHandler wrap(PathHandler chain);
}
