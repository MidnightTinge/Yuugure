package com.mtinge.yuugure.data.postgres;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.beans.ConstructorProperties;
import java.sql.Timestamp;

public class DBAccount {
  @ColumnName("id")
  public final int id;
  @ColumnName("username")
  public final String username;
  @ColumnName("email")
  public transient final String email;
  @ColumnName("password")
  public transient final String password;
  /**
   * A bitfield denoting various user states, e.g. banned, upload restricted, or anything else
   * needed in the future (field space permitting).
   *
   * @see com.mtinge.yuugure.core.States
   */
  @ColumnName("state")
  public final long state;
  @ColumnName("registered")
  public final Timestamp registered;

  @ConstructorProperties({"id", "username", "email", "password", "state", "registered"})
  public DBAccount(int id, String username, String email, String password, long state, Timestamp registered) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.password = password;
    this.state = state;
    this.registered = registered;
  }
}
