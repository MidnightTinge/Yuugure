package com.mtinge.QueryBuilder.ops.order;

public enum OrderType {
  ASC("ASC"),
  DESC("DESC");

  private String token;

  OrderType(String token) {
    this.token = token;
  }

  public String getToken() {
    return token;
  }
}
