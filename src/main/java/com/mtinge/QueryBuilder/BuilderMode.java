package com.mtinge.QueryBuilder;

enum BuilderMode {
  SELECT("SELECT"),
  UPDATE("UPDATE"),
  INSERT("INSERT"),
  DELETE("DELETE");

  private final String token;

  BuilderMode(String token) {
    this.token = token;
  }

  public String getToken() {
    return this.token;
  }
}
