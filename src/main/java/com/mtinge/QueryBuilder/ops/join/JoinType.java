package com.mtinge.QueryBuilder.ops.join;

public enum JoinType {
  FULL_OUTER("FULL OUTER"),
  INNER("INNER"),
  LEFT_OUTER("LEFT OUTER"),
  RIGHT_OUTER("RIGHT OUTER");
  private String token;

  JoinType(String token) {
    this.token = token;
  }

  public String getToken() {
    return token;
  }
}
