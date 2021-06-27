package com.mtinge.yuugure.core.TagManager;

import com.mtinge.yuugure.data.postgres.DBTag;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MutableTag {
  public int id;
  public Integer parent;
  public String category;
  public String name;
  public String assocType;
  public Integer assocId;

  public DBTag toDb() {
    return new DBTag(id, parent, category, name, assocType, assocId);
  }

  public static MutableTag fromDb(DBTag tag) {
    return new MutableTag(tag.id, tag.parent, tag.category, tag.name, tag.assocType, tag.assocId);
  }

}
