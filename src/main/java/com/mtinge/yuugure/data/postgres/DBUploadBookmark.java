package com.mtinge.yuugure.data.postgres;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.beans.ConstructorProperties;
import java.sql.Timestamp;

public final class DBUploadBookmark {
  @ColumnName("active")
  public final boolean active;
  @ColumnName("timestamp")
  public final Timestamp timestamp;
  @ColumnName("public")
  public final boolean isPublic;
  @ColumnName("upload")
  public final int upload;
  @ColumnName("account")
  public final int account;

  @ConstructorProperties({"active", "timestamp", "public", "upload", "account"})
  public DBUploadBookmark(boolean active, Timestamp timestamp, boolean isPublic, int upload, int account) {
    this.active = active;
    this.timestamp = timestamp;
    this.isPublic = isPublic;
    this.upload = upload;
    this.account = account;
  }
}
