package com.mtinge.yuugure.data.http;

import com.mtinge.yuugure.data.postgres.DBTag;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SafeTag {
  public final int id;
  public final Integer parent;
  public final String category;
  public final String name;

  public static SafeTag fromDb(DBTag tag) {
    return new SafeTag(tag.id, tag.parent, tag.category, tag.name);
  }

}
