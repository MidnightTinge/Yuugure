package com.mtinge.yuugure.data.postgres;

import lombok.AllArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;

@AllArgsConstructor
public class DBMedia {
  public final int id;
  public final String sha256;
  public final String md5;
  public final String phash;
  public final String mime;

  public static RowMapper<DBMedia> Mapper = (r, c) ->
    new DBMedia(
      r.getInt("id"),
      r.getString("sha256"),
      r.getString("md5"),
      r.getString("phash"),
      r.getString("mime")
    );
}
