package com.mtinge.yuugure.services.http.api;

import com.mtinge.RateLimit.Limiter;
import com.mtinge.yuugure.data.http.Response;
import com.mtinge.yuugure.data.postgres.DBAccount;
import com.mtinge.yuugure.services.http.Responder;
import com.mtinge.yuugure.services.http.handlers.AddressHandler;
import com.mtinge.yuugure.services.http.handlers.SessionHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.PathTemplateMatch;
import io.undertow.util.StatusCodes;

public abstract class APIResource<T> {
  protected boolean checkRatelimit(HttpServerExchange exchange, Limiter limiter) {
    var res = limiter.check(exchange.getAttachment(AddressHandler.ATTACHMENT_KEY));
    var responder = Responder.with(exchange);
    responder.ratelimitHeaders(res);

    if (res.overLimit) {
      if (res.panicWorthy) {
        // TODO alert someone, when panicWorthy=true we've already panicked. can be done when we add
        //      prometheus bindings.
      }
      responder.ratelimited(res);
      return false;
    }

    return true;
  }

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

  protected String extractForm(HttpServerExchange exchange, String key) {
    return extractForm(exchange.getAttachment(FormDataParser.FORM_DATA), key);
  }

  protected String extractForm(FormData data, String key) {
    if (data != null && data.contains(key)) {
      var frm = data.getFirst(key);
      if (!frm.isFileItem()) {
        return frm.getValue();
      }
    }

    return "";
  }

  protected String extractConfirmationToken(HttpServerExchange exchange) {
    var form = exchange.getAttachment(FormDataParser.FORM_DATA);
    if (form != null && form.contains("confirmation_token")) {
      var frmToken = form.getFirst("confirmation_token");
      if (!frmToken.isFileItem()) {
        return frmToken.getValue();
      }
    }

    return null;
  }

  protected abstract ResourceResult<T> fetchResource(HttpServerExchange exchange);
}
