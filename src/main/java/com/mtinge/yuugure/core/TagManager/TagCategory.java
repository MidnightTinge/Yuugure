package com.mtinge.yuugure.core.TagManager;

public enum TagCategory {
  DIMENSIONS("dimensions"),
  FILESIZE("filesize"),
  LENGTH("length"),
  META("meta"),
  MISC("misc"),
  RATING("rating"),
  USERLAND("userland");

  String name;

  TagCategory(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String toString() {
    return name;
  }
}
