package com.mtinge.yuugure.data.http;

import com.mtinge.yuugure.data.postgres.DBAccount;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SafeAccount {
  public final int id;
  public final String username;

  public static SafeAccount fromDb(DBAccount dbAccount) {
    return new SafeAccount(dbAccount.id, dbAccount.username);
  }
}
