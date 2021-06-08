package com.mtinge.yuugure.data.postgres;

import lombok.AllArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;

import java.sql.Timestamp;

@AllArgsConstructor
public final class DBSession {
  public final int id;
  public final String token;
  public final int account;
  public final Timestamp created;
  public final Timestamp expires;

  public static final RowMapper<DBSession> Mapper = (r, ctx) ->
    new DBSession(
      r.getInt("id"),
      r.getString("token"),
      r.getInt("account"),
      r.getTimestamp("created"),
      r.getTimestamp("expires")
    );
}
