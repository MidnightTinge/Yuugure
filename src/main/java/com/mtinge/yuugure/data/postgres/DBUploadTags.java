package com.mtinge.yuugure.data.postgres;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.beans.ConstructorProperties;

public final class DBUploadTags {
  @ColumnName("upload")
  public final int upload;
  @ColumnName("tag")
  public final int tag;

  @ConstructorProperties({"upload", "tag"})
  public DBUploadTags(int upload, int tag) {
    this.upload = upload;
    this.tag = tag;
  }
}
