package com.mtinge.yuugure.services.http.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.HttpString;

/**
 * An HTTP handler that intercepts the "method" from an HTTP form and overwrites the current
 * request's HTTP method.<br /> When something like the following is invoked:
 * <pre>
 *   &lt;form action="/account/delete" method="POST"&gt;
 *     &lt;input type="hidden" name="_method" value="DELETE" /&gt;
 *     &lt;button type="submit">Delete Account&lt;/button&gt;
 *   &lt;/form&gt;
 * </pre>
 * we then expect the resulting HTTP request to be transformed into a "DELETE" method.
 */
public class FormMethodHandler implements HttpHandler {
  private final HttpHandler next;

  public FormMethodHandler(HttpHandler next) {
    this.next = next;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    var form = exchange.getAttachment(FormDataParser.FORM_DATA);
    if (form != null) {
      String method = null;
      if (form.contains("method")) {
        method = form.getFirst("method").getValue();
      } else if (form.contains("_method")) {
        method = form.getFirst("_method").getValue();
      }
      if (method != null) {
        var toSet = HttpString.tryFromString(method);
        if (toSet != null) {
          exchange.setRequestMethod(toSet);
        }
      }
    }
    next.handleRequest(exchange);
  }
}
