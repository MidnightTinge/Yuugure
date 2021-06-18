package com.mtinge.yuugure.services.http.api;

import com.mtinge.yuugure.data.http.Response;
import com.mtinge.yuugure.data.postgres.DBAccount;
import com.mtinge.yuugure.services.http.Responder;
import com.mtinge.yuugure.services.http.handlers.SessionHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.util.PathTemplateMatch;
import io.undertow.util.StatusCodes;

public abstract class APIResource<T> {

  protected void sendTerminalForState(HttpServerExchange exchange, FetchState state) {
    var res = Responder.with(exchange);
    switch (state) {
      case NOT_FOUND -> res.status(StatusCodes.NOT_FOUND).json(Response.bad(StatusCodes.NOT_FOUND, StatusCodes.NOT_FOUND_STRING));
      case UNAUTHORIZED -> res.status(StatusCodes.UNAUTHORIZED).json(Response.bad(StatusCodes.UNAUTHORIZED, StatusCodes.UNAUTHORIZED_STRING));
      default -> res.status(StatusCodes.INTERNAL_SERVER_ERROR).json(Response.bad(StatusCodes.INTERNAL_SERVER_ERROR, StatusCodes.INTERNAL_SERVER_ERROR_STRING));
    }
  }

  public abstract PathTemplateHandler getRoutes();

  protected DBAccount getAuthed(HttpServerExchange exchange) {
    return exchange.getAttachment(SessionHandler.ATTACHMENT_KEY);
  }

  protected String extract(HttpServerExchange exchange, String key) {
    return extract(exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY), key);
  }

  protected String extract(PathTemplateMatch match, String key) {
    return match == null ? "" : match.getParameters().getOrDefault(key, "");
  }

  protected Integer extractInt(HttpServerExchange exchange, String key) {
    return extractInt(exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY), key);
  }

  protected Integer extractInt(PathTemplateMatch match, String key) {
    var extracted = extract(match, key);
    if (!extracted.isBlank() && extracted.matches("^[0-9]+$")) {
      return Integer.parseInt(extracted);
    }

    return null;
  }

  protected abstract ResourceResult<T> fetchResource(HttpServerExchange exchange);
}
