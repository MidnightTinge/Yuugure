package com.mtinge.yuugure.data.postgres;

import lombok.AllArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;

import java.sql.Timestamp;

@AllArgsConstructor
public final class DBReport {
  public final int id;
  public final boolean active;
  public final int account;
  public final Timestamp timestamp;
  public final boolean claimed;
  public final int claimedBy;
  public final String targetType;
  public final int targetId;
  public final String content;

  public static RowMapper<DBReport> Mapper = (r, ctx) ->
    new DBReport(
      r.getInt("id"),
      r.getBoolean("active"),
      r.getInt("account"),
      r.getTimestamp("timestamp"),
      r.getBoolean("claimed"),
      r.getInt("claimed_by"),
      r.getString("target_type"),
      r.getInt("target_id"),
      r.getString("content")
    );
}
