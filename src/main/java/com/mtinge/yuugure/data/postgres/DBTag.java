package com.mtinge.yuugure.data.postgres;

import lombok.AllArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;

@AllArgsConstructor
public final class DBTag {
  public final int id;
  public final String type;
  public final String name;
  public final Integer parent;

  public static final RowMapper<DBTag> Mapper = (r, ctx) -> new DBTag(
    r.getInt("id"),
    r.getString("type"),
    r.getString("name"),
    r.getInt("parent")
  );
}
