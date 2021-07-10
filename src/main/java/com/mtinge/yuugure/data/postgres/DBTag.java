package com.mtinge.yuugure.data.postgres;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.beans.ConstructorProperties;

public final class DBTag {
  @ColumnName("id")
  public final int id;
  @ColumnName("parent")
  public final Integer parent;
  @ColumnName("category")
  public final String category;
  @ColumnName("name")
  public final String name;
  @ColumnName("assoc_type")
  public final String assocType;
  @ColumnName("assoc_id")
  public final Integer assocId;

  @ConstructorProperties({"id", "parent", "category", "name", "assoc_type", "assoc_id"})
  public DBTag(int id, Integer parent, String category, String name, String assocType, Integer assocId) {
    this.id = id;
    this.parent = parent;
    this.category = category;
    this.name = name;
    this.assocType = assocType;
    this.assocId = assocId;
  }
}
