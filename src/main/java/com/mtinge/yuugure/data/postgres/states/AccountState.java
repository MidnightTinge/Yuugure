package com.mtinge.yuugure.data.postgres.states;

import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.data.postgres.DBAccount;
import com.squareup.moshi.Json;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AccountState {
  @Json(name = "DEACTIVATED")
  public final boolean deactivated;
  @Json(name = "DELETED")
  public final boolean deleted;
  @Json(name = "BANNED")
  public final boolean banned;
  @Json(name = "UPLOAD_RESTRICTED")
  public final boolean uploadRestricted;
  @Json(name = "COMMENTS_RESTRICTED")
  public final boolean commentsRestricted;
  @Json(name = "TRUSTED_UPLOADS")
  public final boolean trustedUploads;
  @Json(name = "PRIVATE")
  public final boolean isPrivate;

  public AccountState(DBAccount account) {
    this.deactivated = States.flagged(account.state, States.Account.DEACTIVATED);
    this.deleted = States.flagged(account.state, States.Account.DELETED);
    this.banned = States.flagged(account.state, States.Account.BANNED);
    this.uploadRestricted = States.flagged(account.state, States.Account.UPLOAD_RESTRICTED);
    this.commentsRestricted = States.flagged(account.state, States.Account.COMMENTS_RESTRICTED);
    this.trustedUploads = States.flagged(account.state, States.Account.TRUSTED_UPLOADS);
    this.isPrivate = States.flagged(account.state, States.Account.PRIVATE);
  }

  public static AccountState fromDb(DBAccount account) {
    return new AccountState(account);
  }

}
