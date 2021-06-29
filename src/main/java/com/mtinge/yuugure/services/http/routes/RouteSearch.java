package com.mtinge.yuugure.services.http.routes;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.data.http.Response;
import com.mtinge.yuugure.services.http.Responder;
import com.mtinge.yuugure.services.http.handlers.SessionHandler;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.StatusCodes;

public class RouteSearch extends Route {
  private final PathHandler pathHandler;

  public RouteSearch() {
    super();
    pathHandler = Handlers.path()
      .addPrefixPath("/", this::handleSearch);
  }

  private void handleSearch(HttpServerExchange exchange) {
    var res = Responder.with(exchange);
    if (res.wantsJson()) {
      var qp = exchange.getQueryParameters().get("q");
      if (qp != null && !qp.isEmpty()) {
        var q = qp.getFirst();
        if (q != null && !q.isBlank()) {
          res.json(Response.good().addData(App.database().getUploadsForSearch(App.elastic().search(q), exchange.getAttachment(SessionHandler.ATTACHMENT_KEY))));
        } else {
          res.status(StatusCodes.BAD_REQUEST).json(Response.fromCode(StatusCodes.BAD_REQUEST).addMessage("Missing search query"));
        }
      } else {
        res.status(StatusCodes.BAD_REQUEST).json(Response.fromCode(StatusCodes.BAD_REQUEST).addMessage("Missing search query"));
      }
    } else {
      // the view will send another request when it loads
      res.view("app");
    }
  }

  @Override
  public PathHandler wrap(PathHandler chain) {
    return chain.addPrefixPath("/search", pathHandler);
  }
}
