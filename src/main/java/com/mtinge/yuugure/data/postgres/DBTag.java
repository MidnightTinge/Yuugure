package com.mtinge.yuugure.data.postgres;

import lombok.AllArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;

@AllArgsConstructor
public final class DBTag {
  public final int id;
  public final Integer parent;
  public final String category;
  public final String name;
  public final String assocType;
  public final Integer assocId;

  public static final RowMapper<DBTag> Mapper = (r, ctx) -> new DBTag(
    r.getInt("id"),
    r.getInt("parent"),
    r.getString("category"),
    r.getString("name"),
    r.getString("assoc_type"),
    r.getInt("assoc_id")
  );
}
