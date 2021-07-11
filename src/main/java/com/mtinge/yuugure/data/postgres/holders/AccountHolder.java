package com.mtinge.yuugure.data.postgres.holders;

import com.mtinge.yuugure.data.postgres.DBAccount;
import com.mtinge.yuugure.data.postgres.states.AccountRoles;
import com.mtinge.yuugure.data.postgres.states.AccountState;

public class AccountHolder extends DBAccount {
  public final AccountState accountState;
  public final AccountRoles accountRoles;

  public AccountHolder(DBAccount account) {
    super(account.id, account.username, account.email, account.password, account.state, account.registered, account.roles);
    this.accountState = AccountState.fromDb(account);
    this.accountRoles = AccountRoles.fromDb(account);
  }

  /**
   * @return Whether or not the user can create new uploads.
   */
  public boolean canUpload() {
    return !(accountState.banned || accountState.uploadRestricted);
  }

  /**
   * @return Whether or not the user can post new comments.
   */
  public boolean canComment() {
    return !(accountState.banned || accountState.commentsRestricted);
  }

  /**
   * Return whether or not this user can perform actions requiring moderator permissions. This is
   * <code>true</code> if they have either the <code>ADMIN</code> or <code>MOD</code> roles.
   *
   * @return Whether or not this user can perform actions requiring moderator permissions.
   */
  public boolean hasModPerms() {
    return accountRoles.admin || accountRoles.mod;
  }

  /**
   * Return whether or not this user can perform actions requiring administrator permissions. This
   * is <code>true</code> only when they have the <code>ADMIN</code> role.
   *
   * @return Whether or not this user can perform actions requiring administrator permissions
   */
  public boolean hasAdminPerms() {
    return accountRoles.admin;
  }
}
