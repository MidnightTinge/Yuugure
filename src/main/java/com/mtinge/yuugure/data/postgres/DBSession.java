package com.mtinge.yuugure.data.postgres;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.beans.ConstructorProperties;
import java.sql.Timestamp;

public final class DBSession {
  @ColumnName("id")
  public final int id;
  @ColumnName("token")
  public final String token;
  @ColumnName("account")
  public final int account;
  @ColumnName("created")
  public final Timestamp created;
  @ColumnName("expires")
  public final Timestamp expires;

  @ConstructorProperties({"id", "token", "account", "created", "expires"})
  public DBSession(int id, String token, int account, Timestamp created, Timestamp expires) {
    this.id = id;
    this.token = token;
    this.account = account;
    this.created = created;
    this.expires = expires;
  }
}
