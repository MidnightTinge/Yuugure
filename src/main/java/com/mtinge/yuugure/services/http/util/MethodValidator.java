package com.mtinge.yuugure.services.http.util;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MethodValidator {
  public static boolean handleMethodValidation(HttpServerExchange exchange, HttpString... methods) {
    var reqMethod = exchange.getRequestMethod();

    if (reqMethod.equals(Methods.OPTIONS)) {
      _send(exchange, StatusCodes.NO_CONTENT, methods);
    } else {
      var found = false;
      for (var method : methods) {
        if (reqMethod.equals(method)) {
          found = true;
          break;
        }
      }

      if (found) {
        return true;
      }
      _send(exchange, StatusCodes.METHOD_NOT_ALLOWED, methods);
    }

    return false;
  }

  public static void sendNotAllowed(HttpServerExchange exchange, HttpString... methods) {
    _send(exchange, StatusCodes.METHOD_NOT_ALLOWED, methods);
  }

  private static void _send(HttpServerExchange exchange, int statusCode, HttpString... methods) {
    exchange.getResponseHeaders().put(Headers.ALLOW, makeMethodsString(methods));
    exchange.setStatusCode(statusCode);
    exchange.getResponseSender().send("");
  }

  public static String makeMethodsString(HttpString... methods) {
    return Arrays.stream(methods).map(s -> s.toString().toUpperCase()).collect(Collectors.joining(", "));
  }
}
