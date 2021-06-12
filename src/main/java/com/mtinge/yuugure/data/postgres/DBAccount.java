package com.mtinge.yuugure.data.postgres;

import lombok.AllArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;

import java.sql.Timestamp;

@AllArgsConstructor
public class DBAccount {
  public final int id;
  public final String username;
  public final String email;
  public final String password;
  /**
   * A bitfield denoting various user states, e.g. banned, upload restricted, or anything else
   * needed in the future (field space permitting).
   *
   * @see com.mtinge.yuugure.core.States
   */
  public final long state;
  public final Timestamp registered;

  public static RowMapper<DBAccount> Mapper = (rs, ctx) ->
    new DBAccount(
      rs.getInt("id"),
      rs.getString("username"),
      rs.getString("email"),
      rs.getString("password"),
      rs.getLong("state"),
      rs.getTimestamp("registered")
    );
}
