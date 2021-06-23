package com.mtinge.yuugure.data.postgres;

public enum ReportTargetType {
  ACCOUNT("account"),
  UPLOAD("upload"),
  COMMENT("comment");

  String colVal;

  ReportTargetType(String colVal) {
    this.colVal = colVal;
  }

  public String colVal() {
    return colVal;
  }

  public String toString() {
    return colVal;
  }
}
