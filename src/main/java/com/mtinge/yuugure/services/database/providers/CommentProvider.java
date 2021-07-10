package com.mtinge.yuugure.services.database.providers;

import com.mtinge.QueryBuilder.QueryBuilder;
import com.mtinge.QueryBuilder.ops.filter.Filter;
import com.mtinge.QueryBuilder.ops.order.OrderType;
import com.mtinge.yuugure.App;
import com.mtinge.yuugure.data.http.BulkRenderableComment;
import com.mtinge.yuugure.data.http.RenderableComment;
import com.mtinge.yuugure.data.http.SafeAccount;
import com.mtinge.yuugure.data.http.SafeComment;
import com.mtinge.yuugure.data.postgres.DBAccount;
import com.mtinge.yuugure.data.postgres.DBComment;
import com.mtinge.yuugure.data.postgres.DBUpload;
import com.mtinge.yuugure.services.database.Database;
import com.mtinge.yuugure.services.database.props.CommentProps;
import com.mtinge.yuugure.services.database.results.Result;
import org.jdbi.v3.core.Handle;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

public class CommentProvider extends Provider<DBComment, CommentProps> {
  @Override
  public Result<DBComment> create(CommentProps props, Handle handle) {
    return Result.fromValue(
      Database.firstOrNull(
        QueryBuilder.insert("comment")
          .columns("target_type", "target_id", "account", "content_raw", "content_rendered", "parent", "active", "timestamp")
          .values(":type", ":id", ":account", ":raw", ":rendered", ":parent", ":active", ":timestamp")
          .returning("*")
          .bind("parent", props.parent())
          .bind("active", requireNonNullElse(props.active(), true))
          .bind("timestamp", requireNonNullElse(props.timestamp(), Timestamp.from(Instant.now())))
          .bind("type", requireNonNull(props.targetType()))
          .bind("id", requireNonNull(props.targetId()))
          .bind("account", requireNonNull(props.account()))
          .bind("raw", requireNonNull(props.contentRaw()))
          .bind("rendered", requireNonNull(props.contentRendered()))
          .toQuery(handle),
        DBComment.Mapper
      )
    );
  }

  public Result<DBComment> create(@NotNull DBUpload upload, @NotNull DBAccount poster, String raw, String rendered, Handle handle) {
    return create(new CommentProps(null, poster.id, true, Timestamp.from(Instant.now()), DBComment.TYPE_UPLOAD, upload.id, raw, rendered), handle);
  }

  @Override
  public DBComment read(int id, Handle handle) {
    return Database.firstOrNull(
      QueryBuilder.select("*")
        .from("comment")
        .where("id", ":id")
        .bind("id", id)
        .toQuery(handle),
      DBComment.Mapper
    );
  }

  public List<DBComment> readForUpload(DBUpload upload, boolean includeBadFlagged, Handle handle) {
    var query = QueryBuilder.select("*")
      .from("comment")
      .order("timestamp", OrderType.DESC)
      .bind("type", DBComment.TYPE_UPLOAD)
      .bind("id", upload.id);

    var filter = Filter.and(
      Filter.of("target_type", ":type"),
      Filter.of("target_id", ":id")
    );

    if (!includeBadFlagged) {
      filter.append(Filter.of("active"));
    }

    return Database.toList(
      query.where(filter).toQuery(handle),
      DBComment.Mapper
    );
  }

  @Override
  public Result<DBComment> update(int id, CommentProps updated, Handle handle) {
    var query = QueryBuilder.update("comment").where("id", ":id").returning("*").bind("id", id);

    if (updated.parent() != null) {
      query.set("parent", ":parent").bind("parent", updated.parent());
    }

    if (updated.account() != null) {
      query.set("account", ":account").bind("account", updated.account());
    }

    if (updated.active() != null) {
      query.set("active", ":active").bind("active", updated.active());
    }

    if (updated.timestamp() != null) {
      query.set("timestamp", ":timestamp").bind("timestamp", updated.timestamp());
    }

    if (updated.targetType() != null) {
      query.set("targetType", ":targetType").bind("targetType", updated.targetType());
    }

    if (updated.targetId() != null) {
      query.set("targetId", ":targetId").bind("targetId", updated.targetId());
    }

    if (updated.contentRaw() != null) {
      query.set("contentRaw", ":contentRaw").bind("contentRaw", updated.contentRaw());
    }

    if (updated.contentRendered() != null) {
      query.set("contentRendered", ":contentRendered").bind("contentRendered", updated.contentRendered());
    }

    return Result.fromValue(
      Database.firstOrNull(
        query.toQuery(handle),
        DBComment.Mapper
      )
    );
  }

  @Override
  public Result<DBComment> delete(int id, Handle handle) {
    return Result.fromValue(
      Database.firstOrNull(
        QueryBuilder.update("comment")
          .set("active", ":active")
          .where("id", ":id")
          .returning("*")
          .bind("id", id)
          .bind("active", false)
          .toQuery(handle),
        DBComment.Mapper
      )
    );
  }

  public RenderableComment makeRenderable(DBComment comment, Handle handle) {
    var account = SafeAccount.fromDb(App.database().accounts.read(comment.account, handle));

    return new RenderableComment(comment.id, comment.timestamp, account, comment.contentRaw, comment.contentRendered);
  }

  public BulkRenderableComment makeRenderable(List<DBComment> toRender, Handle handle) {
    var accountCache = new HashMap<Integer, SafeAccount>();

    var comments = new LinkedList<SafeComment>();
    for (var comment : toRender) {
      var account = accountCache.get(comment.account);
      if (account == null) {
        account = SafeAccount.fromDb(App.database().accounts.read(comment.account, handle));
        accountCache.put(comment.account, account);
      }

      comments.add(SafeComment.fromDb(comment));
    }

    return new BulkRenderableComment(accountCache, comments);
  }
}
