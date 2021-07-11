package com.mtinge.yuugure.services.database.providers;

import com.mtinge.QueryBuilder.QueryBuilder;
import com.mtinge.yuugure.data.postgres.*;
import com.mtinge.yuugure.services.database.Database;
import com.mtinge.yuugure.services.database.props.AuditProps;
import com.mtinge.yuugure.services.database.results.Result;
import org.jdbi.v3.core.Handle;

import java.sql.Timestamp;
import java.time.Instant;

import static java.util.Objects.requireNonNull;

public class AuditsProvider extends Provider<DBAudits, AuditProps> {
  @Override
  public Result<DBAudits> create(AuditProps props, Handle handle) {
    return Result.fromValue(
      Database.firstOrNull(
        QueryBuilder.insert("audits")
          .columns("account", "target_type", "target_id", "action", "details", "timestamp")
          .values(":account", ":target_type", ":target_id", ":action", ":details", ":timestamp")
          .returning("*")
          .bind("account", requireNonNull(props.account()))
          .bind("target_type", requireNonNull(props.targetType()))
          .bind("target_id", requireNonNull(props.targetId()))
          .bind("action", requireNonNull(props.action()))
          .bind("details", requireNonNull(props.details()))
          .bind("timestamp", requireNonNull(props.timestamp()))
          .toQuery(handle),
        DBAudits.class
      )
    );
  }

  @Override
  public DBAudits read(int id, Handle handle) {
    return Database.firstOrNull(
      QueryBuilder.select("*")
        .from("audits")
        .where("id", ":id")
        .bind("id", id)
        .toQuery(handle),
      DBAudits.class
    );
  }

  public Result<DBAudits> trackAction(DBAccount actor, DBUpload target, String action, String details, Handle handle) {
    return create(new AuditProps(
      actor.id,
      "upload",
      target.id,
      action,
      details,
      Timestamp.from(Instant.now())
    ), handle);
  }

  public Result<DBAudits> trackAction(DBAccount actor, DBAccount target, String action, String details, Handle handle) {
    return create(new AuditProps(
      actor.id,
      "account",
      target.id,
      action,
      details,
      Timestamp.from(Instant.now())
    ), handle);
  }

  public Result<DBAudits> trackAction(DBAccount actor, DBComment target, String action, String details, Handle handle) {
    return create(new AuditProps(
      actor.id,
      "comment",
      target.id,
      action,
      details,
      Timestamp.from(Instant.now())
    ), handle);
  }

  public Result<DBAudits> trackAction(DBAccount actor, DBReport target, String action, String details, Handle handle) {
    return create(new AuditProps(
      actor.id,
      "report",
      target.id,
      action,
      details,
      Timestamp.from(Instant.now())
    ), handle);
  }

  public Result<DBAudits> trackAction(DBAccount actor, DBMedia target, String action, String details, Handle handle) {
    return create(new AuditProps(
      actor.id,
      "media",
      target.id,
      action,
      details,
      Timestamp.from(Instant.now())
    ), handle);
  }

  @Override
  public Result<DBAudits> update(int id, AuditProps updated, Handle handle) {
    throw new Error("Operation Not Supported");
  }

  @Override
  public Result<DBAudits> delete(int id, Handle handle) {
    throw new Error("Operation Not Supported");
  }
}
