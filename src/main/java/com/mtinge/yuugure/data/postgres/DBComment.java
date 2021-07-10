package com.mtinge.yuugure.data.postgres;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.beans.ConstructorProperties;
import java.sql.Timestamp;

public final class DBComment {
  public static final String TYPE_UPLOAD = "upload";

  @ColumnName("id")
  public final int id;
  @ColumnName("parent")
  public final Integer parent;
  @ColumnName("account")
  public final int account;
  @ColumnName("active")
  public final boolean active;
  @ColumnName("timestamp")
  public final Timestamp timestamp;
  @ColumnName("target_type")
  public final String targetType;
  @ColumnName("target_id")
  public final int targetId;
  @ColumnName("content_raw")
  public final String contentRaw;
  @ColumnName("content_rendered")
  public final String contentRendered;

  @ConstructorProperties({"id", "parent", "account", "active", "timestamp", "target_type", "target_id", "content_raw", "content_rendered"})
  public DBComment(int id, Integer parent, int account, boolean active, Timestamp timestamp, String targetType, int targetId, String contentRaw, String contentRendered) {
    this.id = id;
    this.parent = parent;
    this.account = account;
    this.active = active;
    this.timestamp = timestamp;
    this.targetType = targetType;
    this.targetId = targetId;
    this.contentRaw = contentRaw;
    this.contentRendered = contentRendered;
  }
}
