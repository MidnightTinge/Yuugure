package com.mtinge.yuugure.services.http.handlers;

import com.mtinge.yuugure.data.http.views.ViewAndModel;
import com.mtinge.yuugure.services.http.Responder;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.util.HashMap;
import java.util.Map;

public class ViewHandler implements HttpHandler {
  private static final Map<String, Object> defaultModel = new HashMap<>();

  private String viewName;
  private Map<String, Object> model;

  public ViewHandler(String viewName) {
    this.viewName = viewName;
    this.model = defaultModel;
  }

  public ViewHandler(ViewAndModel<?> viewAndModel) {
    this.viewName = viewAndModel.getView();
    this.model = viewAndModel.getModel();
  }

  public Map<String, Object> buildModel(HttpServerExchange exchange) {
    return this.model;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    Responder.with(exchange).view(viewName, buildModel(exchange));
  }
}
