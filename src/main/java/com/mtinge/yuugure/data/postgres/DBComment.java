package com.mtinge.yuugure.data.postgres;

import lombok.AllArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;

import java.sql.Timestamp;

@AllArgsConstructor
public final class DBComment {
  public static final String TYPE_UPLOAD = "upload";

  public final int id;
  public final Integer parent;
  public final int account;
  public final boolean active;
  public final Timestamp timestamp;
  public final String targetType;
  public final int targetId;
  public final String contentRaw;
  public final String contentRendered;

  public static final RowMapper<DBComment> Mapper = (r, c) -> new DBComment(
    r.getInt("id"),
    r.getInt("parent"),
    r.getInt("account"),
    r.getBoolean("active"),
    r.getTimestamp("timestamp"),
    r.getString("target_type"),
    r.getInt("target_id"),
    r.getString("content_raw"),
    r.getString("content_rendered")
  );

}
