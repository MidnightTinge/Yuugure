package com.mtinge.yuugure.data.postgres;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.beans.ConstructorProperties;
import java.sql.Timestamp;

public final class DBReport {
  @ColumnName("id")
  public final int id;
  @ColumnName("active")
  public final boolean active;
  @ColumnName("account")
  public final int account;
  @ColumnName("timestamp")
  public final Timestamp timestamp;
  @ColumnName("claimed")
  public final boolean claimed;
  @ColumnName("claimed_by")
  public final int claimedBy;
  @ColumnName("target_type")
  public final String targetType;
  @ColumnName("target_id")
  public final int targetId;
  @ColumnName("content")
  public final String content;

  @ConstructorProperties({"id", "active", "account", "timestamp", "claimed", "claimed_by", "target_type", "target_id", "content"})
  public DBReport(int id, boolean active, int account, Timestamp timestamp, boolean claimed, int claimedBy, String targetType, int targetId, String content) {
    this.id = id;
    this.active = active;
    this.account = account;
    this.timestamp = timestamp;
    this.claimed = claimed;
    this.claimedBy = claimedBy;
    this.targetType = targetType;
    this.targetId = targetId;
    this.content = content;
  }
}
