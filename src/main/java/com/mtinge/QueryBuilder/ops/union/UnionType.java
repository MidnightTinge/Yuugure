package com.mtinge.QueryBuilder.ops.union;

public enum UnionType {
  UNION("UNION"),
  INTERSECT("INTERSECT"),
  EXCEPT("EXCEPT");

  private String token;

  UnionType(String token) {
    this.token = token;
  }

  public String getToken() {
    return token;
  }
}
