package com.mtinge.QueryBuilder.ops.union;

public enum UnionSpecifier {
  ALL("ALL"),
  DISTINCT("DISTINCT");

  private String token;

  UnionSpecifier(String token) {
    this.token = token;
  }

  public String getToken() {
    return token;
  }
}
