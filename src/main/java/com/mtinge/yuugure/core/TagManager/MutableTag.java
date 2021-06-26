package com.mtinge.yuugure.core.TagManager;

import com.mtinge.yuugure.data.postgres.DBTag;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MutableTag {
  public int id;
  public String type;
  public String name;
  public Integer parent;

  public DBTag toDb() {
    return new DBTag(id, type, name, parent);
  }

  public static MutableTag fromDb(DBTag tag) {
    return new MutableTag(tag.id, tag.type, tag.name, tag.parent);
  }

}
