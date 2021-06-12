package com.mtinge.yuugure.data.postgres;

import lombok.AllArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;

import java.sql.Timestamp;

@AllArgsConstructor
public class DBProcessingQueue {
  public final int id;
  public final int upload;
  public final Timestamp queuedAt;
  public final boolean dequeued;
  public final boolean errored;
  public final String errorText;
  public final boolean finished;

  public static final RowMapper<DBProcessingQueue> Mapper = (r, ctx) -> new DBProcessingQueue(
    r.getInt("id"),
    r.getInt("upload"),
    r.getTimestamp("queuedAt"),
    r.getBoolean("dequeued"),
    r.getBoolean("errored"),
    r.getString("error_text"),
    r.getBoolean("finished")
  );
}
