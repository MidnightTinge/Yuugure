package com.mtinge.yuugure.services.http.routes;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.PrometheusMetrics;
import com.mtinge.yuugure.data.http.Response;
import com.mtinge.yuugure.data.http.SearchPagination;
import com.mtinge.yuugure.data.http.SearchResult;
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
      var qQuery = exchange.getQueryParameters().get("q");
      var qPage = exchange.getQueryParameters().get("page");
      if (qQuery != null && !qQuery.isEmpty()) {
        int page = 1;
        if (qPage != null && !qPage.isEmpty()) {
          var _page = qPage.getFirst();
          if (_page.matches("^[0-9]+$")) {
            page = Integer.parseInt(_page);
            if (page <= 0) {
              page = 1;
            }
          }
        }

        var q = qQuery.getFirst();
        if (q != null && !q.isBlank()) {
          PrometheusMetrics.SEARCH_TOTAL.labels(String.valueOf(exchange.getAttachment(SessionHandler.ATTACHMENT_KEY) != null)).inc(); // label: authed
          var searchResult = App.elastic().search(q, page);
          if (searchResult != null) {
            var uploads = App.database().getUploadsForSearch(searchResult.hits, exchange.getAttachment(SessionHandler.ATTACHMENT_KEY));
            res.json(Response.good().addData(new SearchResult(new SearchPagination(searchResult.pageCurrent, searchResult.pageMax), uploads)));
          } else {
            res.status(StatusCodes.INTERNAL_SERVER_ERROR).json(Response.fromCode(StatusCodes.INTERNAL_SERVER_ERROR));
          }
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
