package com.mtinge.yuugure.data.postgres;

import lombok.AllArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;

import java.sql.Timestamp;

@AllArgsConstructor
public class DBAudits {
  public final int id;
  public final int account;
  public final String targetType;
  public final String targetId;
  public final String action;
  public final Timestamp tstamp;
  public final String details;

  public static final RowMapper<DBAudits> Mapper = (r, ctx) -> new DBAudits(
    r.getInt("id"),
    r.getInt("account"),
    r.getString("target_type"),
    r.getString("target_id"),
    r.getString("action"),
    r.getTimestamp("tstamp"),
    r.getString("details")
  );
}
