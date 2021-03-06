package com.mtinge.yuugure.services.http;

import com.mtinge.AcceptParser.Mime;
import com.mtinge.RateLimit.CheckResult;
import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.MoshiFactory;
import com.mtinge.yuugure.data.http.RateLimitResponse;
import com.mtinge.yuugure.data.http.Response;
import com.mtinge.yuugure.data.http.views.ViewAndModel;
import com.mtinge.yuugure.services.http.handlers.AcceptsHandler;
import com.squareup.moshi.Moshi;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Responder {
  private static final Moshi moshi = MoshiFactory.create();

  private final HttpServerExchange exchange;
  private boolean customStatus = false;

  public Responder(HttpServerExchange exchange) {
    this.exchange = exchange;
  }

  public static Responder with(HttpServerExchange exchange) {
    return new Responder(exchange);
  }

  public Responder header(HttpString name, long value) {
    return header(name, Long.toString(value));
  }

  public Responder header(HttpString name, String value) {
    this.exchange.getResponseHeaders().add(name, value);
    return this;
  }

  public Responder cookie(Cookie cookie) {
    this.exchange.setResponseCookie(cookie);
    return this;
  }

  public Responder status(int code) {
    this.exchange.setStatusCode(code);
    this.customStatus = true;
    return this;
  }

  public void methodNotAllowed(HttpString... allowedHeaders) {
    if (!customStatus) {
      exchange.setStatusCode(StatusCodes.METHOD_NOT_ALLOWED);
    }
    if (!exchange.getResponseHeaders().contains(Headers.ALLOW)) {
      exchange.getResponseHeaders().put(Headers.ALLOW, Arrays.stream(allowedHeaders).map(s -> s.toString().toUpperCase()).collect(Collectors.joining(", ")));
    }
    json(Response.fromCode(StatusCodes.METHOD_NOT_ALLOWED));
  }

  public void notFound() {
    if (!customStatus) {
      exchange.setStatusCode(StatusCodes.NOT_FOUND);
    }

    if (wantsJson()) {
      json(Response.fromCode(StatusCodes.NOT_FOUND).addMessage("The requested resource could not be found."));
    } else {
      view("404");
    }
  }

  public void notAuthorized() {
    if (!customStatus) {
      exchange.setStatusCode(StatusCodes.UNAUTHORIZED);
    }

    if (wantsJson()) {
      json(Response.fromCode(StatusCodes.UNAUTHORIZED).addMessage("You must be logged in to perform this action."));
    } else {
      view("401");
    }
  }

  public void forbidden() {
    if (!customStatus) {
      exchange.setStatusCode(StatusCodes.FORBIDDEN);
    }

    if (wantsJson()) {
      json(Response.fromCode(StatusCodes.FORBIDDEN).addMessage("You are forbidden from performing this action with your current permissions."));
    } else {
      view("403");
    }
  }

  public Responder ratelimitHeaders(CheckResult result) {
    if (result != null) {
      exchange.getResponseHeaders()
        .add(HttpString.tryFromString("X-RateLimit-Strategy"), "rolling-window")
        .add(HttpString.tryFromString("X-RateLimit-Period"), result.window.toMillis())
        .add(HttpString.tryFromString("X-RateLimit-MinimumWait"), result.nextAvailable.toMillis())
        .add(HttpString.tryFromString("X-RateLimit-Items"), result.itemsInWindow)
        .add(HttpString.tryFromString("X-RateLimit-Maximum"), result.windowMaximum);
    }

    return this;
  }

  public void ratelimited() {
    if (!customStatus) {
      exchange.setStatusCode(StatusCodes.TOO_MANY_REQUESTS);
    }

    json(Response.fromCode(StatusCodes.TOO_MANY_REQUESTS));
  }

  public void ratelimited(CheckResult result) {
    if (!customStatus) {
      exchange.setStatusCode(StatusCodes.TOO_MANY_REQUESTS);
    }

    json(Response.fromCode(StatusCodes.TOO_MANY_REQUESTS).addData(RateLimitResponse.fromCheck(result)));
  }

  public void badRequest() {
    badRequest(null);
  }

  public void badRequest(@Nullable String message) {
    if (exchange.isComplete()) return;

    var response = Response.fromCode(StatusCodes.BAD_REQUEST);
    if (message != null) {
      response.addMessage(message);
    }

    if (!customStatus) {
      exchange.setStatusCode(StatusCodes.BAD_REQUEST);
    }
    json(response);
  }

  public void internalServerError() {
    internalServerError(null);
  }

  public void internalServerError(Throwable e) {
    if (wantsJson()) {
      json(Response.fromCode(StatusCodes.INTERNAL_SERVER_ERROR));
    } else {
      view("500", App.isDebug() ? Map.of("error", e) : Map.of());
    }
  }

  public void view(String viewName) {
    view(viewName, Map.of());
  }

  public void view(ViewAndModel<?> view) {
    view(view.getView(), view.getModel());
  }

  @SneakyThrows
  public void view(String viewName, Map<String, Object> model) {
    if (exchange.isComplete()) return;

    var template = App.webServer().pebble().getTemplate(viewName);
    model = Objects.requireNonNullElse(model, Map.of());

    var writer = new StringWriter();
    template.evaluate(writer, model);

    exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "text/html");
    exchange.getResponseSender().send(writer.toString());
  }

  public void redirect(String redirectTo) {
    exchange.setStatusCode(StatusCodes.FOUND).getResponseHeaders().put(Headers.LOCATION, redirectTo);
    exchange.endExchange();
  }

  public void redirectKeepMethod(String redirectTo) {
    if (exchange.getRequestMethod().equals(Methods.GET) || exchange.getRequestMethod().equals(Methods.HEAD)) {
      exchange.setStatusCode(StatusCodes.FOUND);
    } else {
      exchange.setStatusCode(StatusCodes.TEMPORARY_REDIRECT);
    }

    exchange.getResponseHeaders().put(Headers.LOCATION, redirectTo);
    exchange.endExchange();
  }

  public boolean wantsJson() {
    var attach = exchange.getAttachment(AcceptsHandler.ATTACHMENT_KEY);
    if (attach == null) return false;

    var match = attach.bestMatch(new Mime("application", "json"), new Mime("text", "html"));
    if (match != null) {
      return match.getMime().equals(new Mime("application", "json"));
    }

    return false;
  }

  public void json(Object toSend) {
    if (!(toSend instanceof String)) {
      toSend = moshi.adapter(Object.class).toJson(toSend);
    }

    header(Headers.CONTENT_TYPE, "application/json");
    _send((String) toSend);
  }

  public void raw(Object toSend) {
    if (!(toSend instanceof String)) {
      toSend = moshi.adapter(Object.class).toJson(toSend);
    }
    _send((String) toSend);
  }

  public void end() {
    exchange.getResponseSender().close();
    exchange.endExchange();
  }

  private void _send(String toSend) {
    if (this.exchange.isResponseComplete())
      return;

    exchange.getResponseSender().send(toSend);
  }
}
