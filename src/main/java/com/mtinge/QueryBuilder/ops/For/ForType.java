package com.mtinge.QueryBuilder.ops.For;

public enum ForType {
  UPDATE("UPDATE"),
  NO_KEY_UPDATE("NO KEY UPDATE"),
  SHARE("SHARE"),
  KEY_SHARE("KEY SHARE");

  private String token;

  ForType(String token) {
    this.token = token;
  }

  public String getToken() {
    return token;
  }
}
