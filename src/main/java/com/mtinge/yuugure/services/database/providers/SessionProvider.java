package com.mtinge.yuugure.services.database.providers;

import com.mtinge.QueryBuilder.QueryBuilder;
import com.mtinge.yuugure.data.postgres.DBSession;
import com.mtinge.yuugure.services.database.Database;
import com.mtinge.yuugure.services.database.props.SessionProps;
import com.mtinge.yuugure.services.database.results.Result;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionProvider extends Provider<DBSession, SessionProps> {
  private static final Logger logger = LoggerFactory.getLogger(SessionProvider.class);

  private static String table = "sessions";

  @Override
  public Result<DBSession> create(SessionProps props, Handle handle) {
    requireTransaction(handle);

    return Result.fromValue(
      Database.firstOrNull(
        QueryBuilder.insert(table)
          .columns("token", "account", "expires")
          .values(":token", ":account", ":expires")
          .returning("*")
          .bind("token", props.token())
          .bind("expires", props.expires())
          .bind("account", props.account())
          .toQuery(handle),
        DBSession.Mapper
      )
    );
  }

  @Override
  public DBSession read(int id, Handle handle) {
    return Database.firstOrNull(
      QueryBuilder.select("*")
        .from(table)
        .where("id", ":id")
        .bind("id", id)
        .toQuery(handle),
      DBSession.Mapper
    );
  }

  public DBSession read(String token, Handle handle) {
    return Database.firstOrNull(
      QueryBuilder.select("*")
        .from(table)
        .where("token", ":token")
        .bind("token", token)
        .toQuery(handle),
      DBSession.Mapper
    );
  }

  @Override
  public Result<DBSession> update(int id, SessionProps updated, Handle handle) {
    return null;
  }

  @Override
  public Result<DBSession> delete(int id, Handle handle) {
    var deleted = Database.updated(
      QueryBuilder.delete(table)
        .where("id", ":id")
        .bind("id", id)
        .toUpdate(handle)
    );

    return new Result<>(null, deleted, null);
  }

  public Result<DBSession> delete(String token, Handle handle) {
    var deleted = Database.updated(
      QueryBuilder.delete(table)
        .where("token", ":token")
        .bind("token", token)
        .toUpdate(handle)
    );

    return new Result<>(null, deleted, null);
  }
}
