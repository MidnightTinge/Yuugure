package com.mtinge.yuugure.data.postgres;

import lombok.AllArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;

import java.sql.Timestamp;

@AllArgsConstructor
public final class DBUploadBookmark {
  public final boolean active;
  public final Timestamp timestamp;
  public final boolean isPublic;
  public final int upload;
  public final int account;

  public static final RowMapper<DBUploadBookmark> Mapper = (r, ctx) -> new DBUploadBookmark(
    r.getBoolean("active"),
    r.getTimestamp("timestamp"),
    r.getBoolean("public"),
    r.getInt("upload"),
    r.getInt("account")
  );
}
