package com.mtinge.yuugure.data.http;

import com.mtinge.yuugure.data.postgres.states.AccountRoles;
import com.mtinge.yuugure.data.postgres.states.AccountState;
import com.mtinge.yuugure.data.postgres.DBAccount;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SafeAccount {
  public final int id;
  public final String username;
  public final AccountState state;
  public final AccountRoles roles;

  public static SafeAccount fromDb(DBAccount dbAccount) {
    if (dbAccount == null) return null;
    return new SafeAccount(dbAccount.id, dbAccount.username, AccountState.fromDb(dbAccount), AccountRoles.fromDb(dbAccount));
  }
}
