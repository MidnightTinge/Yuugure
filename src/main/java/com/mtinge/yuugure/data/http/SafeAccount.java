package com.mtinge.yuugure.data.http;

import com.mtinge.yuugure.data.postgres.states.AccountState;
import com.mtinge.yuugure.data.postgres.DBAccount;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SafeAccount {
  public final int id;
  public final String username;
  public final AccountState state;

  public static SafeAccount fromDb(DBAccount dbAccount) {
    if (dbAccount == null) return null;
    return new SafeAccount(dbAccount.id, dbAccount.username, AccountState.fromDb(dbAccount));
  }
}
