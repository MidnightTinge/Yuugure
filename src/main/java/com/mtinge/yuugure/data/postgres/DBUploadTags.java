package com.mtinge.yuugure.data.postgres;

import lombok.AllArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;

@AllArgsConstructor
public final class DBUploadTags {
  public final int upload;
  public final int tag;

  public static final RowMapper<DBUploadTags> Mapper = (r, ctx) -> new DBUploadTags(
    r.getInt("upload"),
    r.getInt("tag")
  );
}
