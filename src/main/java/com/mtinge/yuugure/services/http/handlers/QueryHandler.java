package com.mtinge.yuugure.services.http.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class QueryHandler implements HttpHandler {
  public static final AttachmentKey<Query> ATTACHMENT_KEY = AttachmentKey.create(Query.class);

  private static final Logger logger = LoggerFactory.getLogger(QueryHandler.class);
  private HttpHandler next;

  public QueryHandler(HttpHandler next) {
    this.next = next;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    var params = exchange.getQueryParameters();
    var query = new Query();

    try {
      if (!params.isEmpty()) {
        for (var entry : params.entrySet()) {
          query.addAll(entry.getKey(), entry.getValue());
        }
      }
    } catch (Exception e) {
      logger.error("Failed to handle query parameters", e);
    }

    exchange.putAttachment(ATTACHMENT_KEY, query);
    next.handleRequest(exchange);
  }

  public static final class Query {
    private Map<String, LinkedList<String>> map = new HashMap<>();

    private Query() {
      //
    }

    private void add(String key, String value) {
      map.compute(key, (k, v) -> {
        if (v == null) v = new LinkedList<>();
        v.add(value);

        return v;
      });
    }

    private void addAll(String key, Collection<String> values) {
      map.compute(key, (k, v) -> {
        if (v == null) v = new LinkedList<>();
        v.addAll(values);

        return v;
      });
    }

    public boolean has(String key) {
      return map.containsKey(key) && !map.get(key).isEmpty();
    }

    public LinkedList<String> get(String key) {
      return map.get(key);
    }

    public String getFirst(String key) {
      return has(key) ? map.get(key).getFirst() : null;
    }
  }
}
