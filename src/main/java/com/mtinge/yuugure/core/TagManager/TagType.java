package com.mtinge.yuugure.core.TagManager;

public enum TagType {
  USERLAND("userland"),
  META("meta");

  String type;

  TagType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  public String toString() {
    return type;
  }
}
