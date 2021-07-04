package com.mtinge.QueryBuilder.ops.filter;

public enum FilterType {
  AND("AND"),
  OR("OR"),
  OF("OF"),
  NOT("NOT");

  private String token;

  FilterType(String token) {
    this.token = token;
  }

  public String getToken() {
    return token;
  }
}
