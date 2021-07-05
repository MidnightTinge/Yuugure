package com.mtinge.yuugure.services.database.providers;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mtinge.QueryBuilder.QueryBuilder;
import com.mtinge.QueryBuilder.ops.filter.Filter;
import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.core.Strings;
import com.mtinge.yuugure.data.postgres.DBAccount;
import com.mtinge.yuugure.services.database.Database;
import com.mtinge.yuugure.services.database.props.AccountProps;
import com.mtinge.yuugure.services.database.results.Result;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountProvider extends Provider<DBAccount, AccountProps> {
  public static final int FAIL_EXISTING_USERNAME = 1;
  public static final int FAIL_EXISTING_EMAIL = 2;
  public static final int FAIL_NO_SUCH_ACCOUNT = 3;

  private static final Logger logger = LoggerFactory.getLogger(AccountProvider.class);

  @Override
  public Result<DBAccount> create(AccountProps props, Handle handle) {
    requireTransaction(handle);

    var emailExists = Database.firstOrNull(
      handle.createQuery("SELECT EXISTS (SELECT id FROM account WHERE lower(email) = lower(:email)) AS \"exists\"")
        .bind("email", props.email()),
      Database.boolMapper("exists")
    );
    var usernameExists = Database.firstOrNull(
      handle.createQuery("SELECT EXISTS (SELECT id FROM account WHERE lower(username) = lower(:username)) AS \"exists\"")
        .bind("username", props.username()),
      Database.boolMapper("exists")
    );

    if (emailExists == null || usernameExists == null) {
      // An error occurred, we should never see this, but just in case it happens we don't want the
      // default behavior to be confusing to the end-user, e.g. we shouldn't say the username is
      // taken/unavailable.
      logger.error("Failed to create account: 'exists' preflight returned null.");
      return Result.fail(FAIL_SQL, Strings.Generic.INTERNAL_SERVER_ERROR);
    } else if (emailExists || usernameExists) {
      if (emailExists) {
        return Result.fail(FAIL_EXISTING_EMAIL);
      }

      return Result.fail(FAIL_EXISTING_USERNAME);
    } else {
      var hash = BCrypt.withDefaults().hashToString(12, props.password().toCharArray());

      // Insert the account and return a failure if the returned result is null
      return Result.fromValue(
        Database.firstOrNull(
          QueryBuilder.insert("account")
            .columns("username", "email", "password")
            .values(":username", ":email", ":hash")
            .returning("*")
            .bind("username", props.username())
            .bind("email", props.email())
            .bind("hash", hash)
            .toQuery(handle),
          DBAccount.Mapper
        )
      );
    }
  }

  @Override
  public DBAccount read(int id, Handle handle) {
    return Database.firstOrNull(
      QueryBuilder.select("*")
        .from("account")
        .where("id", ":id")
        .bind("id", id)
        .toQuery(handle),
      DBAccount.Mapper
    );
  }

  public DBAccount read(int id, boolean includeBadFlagged, Handle handle) {
    if (includeBadFlagged) {
      return read(id, handle);
    } else {
      return Database.firstOrNull(
        QueryBuilder.select("*")
          .from("account")
          .where(
            Filter.and(
              Filter.of("id", ":id"),
              Filter.of("(state & :badState)", 0)
            )
          )
          .bind("id", id)
          .bind("badState", States.compute(States.Account.PRIVATE, States.Account.DELETED, States.Account.DEACTIVATED))
          .toQuery(handle),
        DBAccount.Mapper
      );
    }
  }

  @Override
  public Result<DBAccount> update(int id, AccountProps updated, Handle handle) {
    var account = read(id, handle);
    if (account != null) {
      var update = QueryBuilder.update("account")
        .where("id", ":id")
        .bind("id", id)
        .returning("*");

      if (updated.password() != null && !updated.password().isBlank()) {
        update
          .set("password", ":hash")
          .bind("hash", BCrypt.withDefaults().hashToString(12, updated.password().toCharArray()));
      }
      if (updated.email() != null && !updated.email().isBlank()) {
        update
          .set("email", ":email")
          .bind("email", updated.email());
      }
      if (updated.username() != null && !updated.username().isBlank()) {
        update
          .set("username", ":username")
          .bind("username", updated.username());
      }
      if (updated.state() != null) {
        update
          .set("state", ":state")
          .bind("state", updated.state());
      }

      var built = update.build();

      var altered = Database.first(
        update.toQuery(handle),
        DBAccount.Mapper
      );
      if (altered != null) {
        return Result.success(altered);
      } else {
        return Result.fail(FAIL_SQL);
      }
    } else {
      return Result.fail(FAIL_NO_SUCH_ACCOUNT);
    }
  }

  @Override
  public Result<DBAccount> delete(int id, Handle handle) {
    requireTransaction(handle);

    var account = read(id, handle);
    if (account != null) {
      // lock objects
      handle.execute("SELECT 1 FROM upload WHERE owner = ? FOR UPDATE", account.id);
      handle.execute("SELECT 1 FROM sessions WHERE account = ? FOR UPDATE", account.id);
      handle.execute("SELECT 1 FROM account WHERE id = ? FOR UPDATE", account.id);

      // set delete states
      handle.createUpdate("UPDATE account SET state = (state | :state) WHERE id = :account")
        .bind("account", account.id)
        .bind("state", States.Account.DELETED)
        .execute();
      handle.createUpdate("UPDATE upload SET state = (state | :state) WHERE owner = :account")
        .bind("account", account.id)
        .bind("state", States.Upload.DELETED)
        .execute();

      // kill sessions
      handle.createUpdate("DELETE FROM sessions WHERE account = :account")
        .bind("account", account.id)
        .execute();

      // job done
      return Result.success(account);
    } else {
      return Result.fail(FAIL_NO_SUCH_ACCOUNT);
    }
  }
}
