package com.mtinge.yuugure.data.postgres.states;

import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.data.postgres.DBAccount;
import com.squareup.moshi.Json;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AccountRoles {
  @Json(name = "ADMIN")
  public final boolean admin;
  @Json(name = "MOD")
  public final boolean mod;

  public AccountRoles(DBAccount account) {
    this.admin = States.flagged(account.roles, States.Roles.ADMIN);
    this.mod = States.flagged(account.roles, States.Roles.MOD);
  }

  public static AccountRoles fromDb(DBAccount account) {
    return new AccountRoles(account);
  }
}
