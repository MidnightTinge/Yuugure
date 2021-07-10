package com.mtinge.yuugure.data.postgres;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.beans.ConstructorProperties;

public final class DBUploadVote {
  @ColumnName("active")
  public final boolean active;
  @ColumnName("is_up")
  public final boolean isUp;
  @ColumnName("upload")
  public final int upload;
  @ColumnName("account")
  public final int account;

  @ConstructorProperties({"active", "is_up", "upload", "account"})
  public DBUploadVote(boolean active, boolean isUp, int upload, int account) {
    this.active = active;
    this.isUp = isUp;
    this.upload = upload;
    this.account = account;
  }
}
