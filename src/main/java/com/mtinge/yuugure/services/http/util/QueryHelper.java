package com.mtinge.yuugure.services.http.util;

import java.util.Deque;
import java.util.Map;

public class QueryHelper {
  private final Map<String, Deque<String>> params;

  public QueryHelper(Map<String, Deque<String>> params) {
    this.params = params;
  }

  public String first(String key) {
    if (params == null) {
      return "";
    }

    if (params.containsKey(key) && !params.get(key).isEmpty()) {
      return params.get(key).getFirst();
    }

    return "";
  }

  public String firstOr(String key, String or) {
    if (params == null) {
      return or;
    }

    var v = first(key);
    if (!v.isBlank()) {
      return v;
    }

    return or;
  }

  public static String first(Map<String, Deque<String>> params, String key) {
    return new QueryHelper(params).first(key);
  }

  public static String firstOr(Map<String, Deque<String>> params, String key, String or) {
    return new QueryHelper(params).firstOr(key, or);
  }

}
