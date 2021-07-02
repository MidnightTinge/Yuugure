package com.mtinge.yuugure.data.postgres;

import lombok.AllArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;

@AllArgsConstructor
public final class DBUploadVote {
  public final boolean active;
  public final boolean isUp;
  public final int upload;
  public final int account;

  public static final RowMapper<DBUploadVote> Mapper = (r, ctx) -> new DBUploadVote(
    r.getBoolean("active"),
    r.getBoolean("is_up"),
    r.getInt("upload"),
    r.getInt("account")
  );
}
