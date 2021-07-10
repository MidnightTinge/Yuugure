package com.mtinge.yuugure.services.database.providers;

import com.mtinge.QueryBuilder.QueryBuilder;
import com.mtinge.QueryBuilder.ops.filter.Filter;
import com.mtinge.QueryBuilder.ops.order.OrderType;
import com.mtinge.yuugure.data.postgres.*;
import com.mtinge.yuugure.services.database.Database;
import com.mtinge.yuugure.services.database.props.ReportProps;
import com.mtinge.yuugure.services.database.results.Result;
import org.jdbi.v3.core.Handle;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class ReportProvider extends Provider<DBReport, ReportProps> {
  @Override
  public Result<DBReport> create(ReportProps props, Handle handle) {
    requireTransaction(handle);

    return Result.fromValue(
      Database.firstOrNull(
        QueryBuilder.insert("report")
          .columns("active", "account", "timestamp", "claimed", "claimed_by", "target_type", "target_id", "content")
          .values(":active", ":account", ":timestamp", ":claimed", ":claimed_by", ":target_type", ":target_id", ":content")
          .bind("active", props.active())
          .bind("account", props.account())
          .bind("timestamp", props.timestamp())
          .bind("claimed", props.claimed())
          .bind("claimed_by", props.claimedBy())
          .bind("target_type", props.targetType().colVal())
          .bind("target_id", props.targetId())
          .bind("content", props.content())
          .returning("*")
          .toQuery(handle),
        DBReport.class
      )
    );
  }

  public Result<DBReport> create(@NotNull DBUpload target, @NotNull DBAccount reporter, String content, Handle handle) {
    return create(new ReportProps(true, reporter.id, Timestamp.from(Instant.now()), false, null, ReportTargetType.UPLOAD, target.id, content), handle);
  }

  public Result<DBReport> create(@NotNull DBAccount target, @NotNull DBAccount reporter, String content, Handle handle) {
    return create(new ReportProps(true, reporter.id, Timestamp.from(Instant.now()), false, null, ReportTargetType.ACCOUNT, target.id, content), handle);
  }

  public Result<DBReport> create(@NotNull DBComment target, @NotNull DBAccount reporter, String content, Handle handle) {
    return create(new ReportProps(true, reporter.id, Timestamp.from(Instant.now()), false, null, ReportTargetType.COMMENT, target.id, content), handle);
  }

  @Override
  public DBReport read(int id, Handle handle) {
    return Database.firstOrNull(
      QueryBuilder.select("*")
        .from("report")
        .where("id", ":id")
        .bind("id", id)
        .toQuery(handle),
      DBReport.class
    );
  }

  public List<DBReport> read(ReportTargetType type, int target, Handle handle) {
    return Database.toList(
      QueryBuilder.select("*")
        .from("report")
        .where(
          Filter.and(
            Filter.of("target_type", ":type"),
            Filter.of("target_id", ":id")
          )
        )
        .order("timestamp", OrderType.DESC)
        .bind("type", type.colVal())
        .bind("target", target)
        .toQuery(handle),
      DBReport.class
    );
  }

  public List<DBReport> read(DBUpload target, Handle handle) {
    return read(ReportTargetType.UPLOAD, target.id, handle);
  }

  public List<DBReport> read(DBComment target, Handle handle) {
    return read(ReportTargetType.COMMENT, target.id, handle);
  }

  public List<DBReport> read(DBAccount target, Handle handle) {
    return read(ReportTargetType.ACCOUNT, target.id, handle);
  }

  @Override
  public Result<DBReport> update(int id, ReportProps updated, Handle handle) {
    var query = QueryBuilder.update("report").where("id", ":id").bind("id", id);

    if (updated.active() != null) {
      query.set("active", ":active").bind("active", updated.active());
    }

    if (updated.account() != null) {
      query.set("account", ":account").bind("account", updated.account());
    }

    if (updated.timestamp() != null) {
      query.set("timestamp", ":timestamp").bind("timestamp", updated.timestamp());
    }

    if (updated.claimed() != null) {
      query.set("claimed", ":claimed").bind("claimed", updated.claimed());
    }

    if (updated.claimedBy() != null) {
      query.set("claimedBy", ":claimedBy").bind("claimedBy", updated.claimedBy());
    }

    if (updated.targetType() != null || updated.targetId() != null) {
      query.set("targetType", ":targetType").bind("targetType", requireNonNull(updated.targetType(), "You must specify targetType when you've specified targetId."));
      query.set("targetId", ":targetId").bind("targetId", requireNonNull(updated.targetId(), "You must specify targetId when you've specified targetType."));
    }

    if (updated.content() != null && !updated.content().isBlank()) {
      query.set("content", ":content").bind("content", updated.content());
    }

    return Result.fromValue(
      Database.firstOrNull(
        query.returning("*").toQuery(handle),
        DBReport.class
      )
    );
  }

  @Override
  public Result<DBReport> delete(int id, Handle handle) {
    throw new Error("Operation Not Supported");
  }
}
