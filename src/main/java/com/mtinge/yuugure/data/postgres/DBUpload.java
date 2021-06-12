package com.mtinge.yuugure.data.postgres;

import lombok.AllArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;

import java.sql.Timestamp;

@AllArgsConstructor
public class DBUpload {
  public final int id;
  public final int media;
  public final int parent;
  public final int owner;
  public final Timestamp uploadDate;
  public final long state;

  public static final RowMapper<DBUpload> Mapper = (r, c) -> new DBUpload(
    r.getInt("id"),
    r.getInt("media"),
    r.getInt("parent"),
    r.getInt("owner"),
    r.getTimestamp("upload_date"),
    r.getLong("state")
  );
}
