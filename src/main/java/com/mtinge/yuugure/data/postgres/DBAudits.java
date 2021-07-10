package com.mtinge.yuugure.data.postgres;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.beans.ConstructorProperties;
import java.sql.Timestamp;

public class DBAudits {
  @ColumnName("id")
  public final int id;
  @ColumnName("account")
  public final int account;
  @ColumnName("target_type")
  public final String targetType;
  @ColumnName("target_id")
  public final String targetId;
  @ColumnName("action")
  public final String action;
  @ColumnName("timestamp")
  public final Timestamp timestamp;
  @ColumnName("details")
  public final String details;

  @ConstructorProperties({"id", "account", "target_type", "target_id", "action", "timestamp", "details"})
  public DBAudits(int id, int account, String targetType, String targetId, String action, Timestamp timestamp, String details) {
    this.id = id;
    this.account = account;
    this.targetType = targetType;
    this.targetId = targetId;
    this.action = action;
    this.timestamp = timestamp;
    this.details = details;
  }
}
