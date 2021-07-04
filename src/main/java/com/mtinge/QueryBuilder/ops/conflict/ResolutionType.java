package com.mtinge.QueryBuilder.ops.conflict;

public enum ResolutionType {
  UPDATE("UPDATE SET"),
  IGNORE("NOTHING");

  private String token;

  ResolutionType(String token) {
    this.token = token;
  }

  public String getToken() {
    return token;
  }
}
