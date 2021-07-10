package com.mtinge.yuugure.services.database.providers;

import com.mtinge.QueryBuilder.FetchBuilder;
import com.mtinge.QueryBuilder.QueryBuilder;
import com.mtinge.QueryBuilder.ops.filter.Filter;
import com.mtinge.QueryBuilder.ops.join.Join;
import com.mtinge.QueryBuilder.ops.order.OrderType;
import com.mtinge.yuugure.App;
import com.mtinge.yuugure.data.http.BulkRenderableUpload;
import com.mtinge.yuugure.data.http.UploadBookmarkState;
import com.mtinge.yuugure.data.postgres.DBAccount;
import com.mtinge.yuugure.data.postgres.DBUpload;
import com.mtinge.yuugure.data.postgres.DBUploadBookmark;
import com.mtinge.yuugure.services.database.BookmarkFetchParams;
import com.mtinge.yuugure.services.database.Database;
import com.mtinge.yuugure.services.database.props.BookmarkProps;
import com.mtinge.yuugure.services.database.results.BookmarkResult;
import com.mtinge.yuugure.services.database.results.Result;
import org.jdbi.v3.core.Handle;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class BookmarkProvider extends Provider<DBUploadBookmark, BookmarkProps> {
  @Override
  public Result<DBUploadBookmark> create(BookmarkProps props, Handle handle) {
    return Result.fromValue(
      Database.firstOrNull(
        QueryBuilder.insert("upload_bookmark")
          .columns("account", "upload", "active", "public", "timestamp")
          .values(":account", ":upload", ":active", ":public", ":timestamp")
          .bind("account", requireNonNull(props.account()))
          .bind("upload", requireNonNull(props.upload()))
          .bind("active", requireNonNull(props.active()))
          .bind("public", requireNonNull(props.isPublic()))
          .bind("timestamp", requireNonNull(props.timestamp()))
          .returning("*")
          .toQuery(handle),
        DBUploadBookmark.class
      )
    );
  }

  @Override
  public DBUploadBookmark read(int id, Handle handle) {
    throw new Error("Operation Not Supported: This table has a double-column key. Use a different read method.");
  }

  public DBUploadBookmark read(int owner, int upload, Handle handle) {
    return Database.firstOrNull(
      QueryBuilder.select("*")
        .from("upload_bookmark")
        .where(Filter.and(
          Filter.of("owner", ":owner"),
          Filter.of("upload", ":upload")
        ))
        .bind("owner", owner)
        .bind("upload", upload)
        .toQuery(handle),
      DBUploadBookmark.class
    );
  }

  public List<DBUploadBookmark> readForUpload(int uploadId, BookmarkFetchParams fetchParams, Handle handle) {
    var builder = QueryBuilder.select("*")
      .from("upload_bookmark")
      .bind("upload", uploadId);
    var filter = Filter.of("upload", ":upload");

    if (!fetchParams.includePrivate || !fetchParams.includeInactive) {
      filter = Filter.and(
        filter,
        !fetchParams.includePrivate ? Filter.of("public") : null,
        !fetchParams.includeInactive ? Filter.of("active") : null
      );
    }

    return builder
      .where(filter)
      .toQuery(handle)
      .mapTo(DBUploadBookmark.class)
      .collect(Collectors.toList());
  }

  @Override
  public Result<DBUploadBookmark> update(int id, BookmarkProps updated, Handle handle) {
    throw new Error("Operation Not Supported: This table has a double-column key. Use a different update method.");
  }

  public Result<DBUploadBookmark> update(int owner, int upload, BookmarkProps updated, Handle handle) {
    var query = QueryBuilder.update("upload_bookmark")
      .where(Filter.and(
        Filter.of("owner", ":owner"),
        Filter.of("upload", ":upload")
      ))
      .returning("*");

    if (updated.active() != null) {
      query.set("active", ":active").bind("active", updated.active());
    }
    if (updated.isPublic() != null) {
      query.set("public", ":public").bind("public", updated.isPublic());
    }
    if (updated.upload() != null) {
      query.set("upload", ":upload").bind("upload", updated.upload());
    }
    if (updated.account() != null) {
      query.set("account", ":account").bind("account", updated.account());
    }
    if (updated.timestamp() != null) {
      query.set("timestamp", ":timestamp").bind("timestamp", updated.timestamp());
    }

    return Result.fromValue(
      Database.firstOrNull(
        query.toQuery(handle),
        DBUploadBookmark.class
      )
    );
  }

  @Override
  public Result<DBUploadBookmark> delete(int id, Handle handle) {
    throw new Error("Operation Not Supported: This table has a double-column key. Use a different delete method.");
  }

  public Result<DBUploadBookmark> delete(int upload, int account, Handle handle) {
    return Result.fromValue(
      Database.firstOrNull(
        QueryBuilder.update("upload_bookmark")
          .set("active", ":false")
          .where(
            Filter.and(
              Filter.of("upload", ":upload"),
              Filter.of("account", ":account")
            )
          )
          .bind("upload", upload)
          .bind("account", account)
          .toQuery(handle),
        DBUploadBookmark.class
      )
    );
  }

  public BookmarkResult handleFlip(DBAccount account, DBUpload upload, BookmarkProps props, Handle handle) {
    requireNonNull(props.isPublic());
    requireNonNull(props.active());

    // lock the existing row for update if it exists
    var existing = handle.createQuery("SELECT * FROM upload_bookmark WHERE account = :account AND upload = :upload FOR UPDATE")
      .bind("account", account.id)
      .bind("upload", upload.id)
      .mapTo(DBUploadBookmark.class)
      .findFirst().orElse(null);

    // Create or update an existing bookmark
    var affected = handle.createUpdate("INSERT INTO upload_bookmark (account, upload, active, public, timestamp) VALUES (:account, :upload, :active, :public, :timestamp) ON CONFLICT ON CONSTRAINT upload_bookmark_pkey DO UPDATE SET account = :account, upload = :upload, active = :active, public = :public, timestamp = :timestamp")
      .bind("account", account.id)
      .bind("upload", upload.id)
      .bind("public", props.isPublic())
      .bind("active", props.active())
      .bind("timestamp", props.timestamp())
      .execute();

    // Job is done at this point - calculate contextual bools for websocket state updates and
    // return.
    boolean isPublic = props.isPublic();
    boolean wasPublic = existing != null && existing.isPublic;

    boolean isActive = props.active();
    boolean wasActive = existing != null && existing.active;

    return new BookmarkResult(affected > 0, isPublic, wasPublic, isActive, wasActive);
  }

  public Integer countForAccount(int accountId, @Nullable DBAccount context, Handle handle) {
    return Database.firstOrNull(
      _bookmarksForAccount(accountId, context != null && context.id == accountId, true).toQuery(handle),
      Database.intMapper("count")
    );
  }

  public BulkRenderableUpload getRenderableBookmarksForAccount(int accountId, @Nullable DBAccount context, Handle handle) {
    var uploads = Database.toList(
      _bookmarksForAccount(accountId, context != null && context.id == accountId, false).toQuery(handle),
      DBUpload.class
    );
    return App.database().uploads.makeUploadsRenderable(uploads, context, handle);
  }

  public BulkRenderableUpload getRenderableBookmarksForAccount(int accountId, @Nullable Integer before, @Nullable DBAccount context, Handle handle) {
    var uploads = Database.toList(
      _bookmarksForAccount(accountId, before, context != null && context.id == accountId).toQuery(handle),
      DBUpload.class
    );
    return App.database().uploads.makeUploadsRenderable(uploads, context, handle);
  }

  private FetchBuilder _bookmarksForAccount(int accountId, boolean includePrivate, boolean countOnly) {
    var builder = QueryBuilder.select(countOnly ? "count(u.id) as \"count\"" : "u.*")
      .from("upload_bookmark b")
      .join(Join.inner("upload u", "b.upload = u.id"))
      .join(Join.inner("account a", "b.account = a.id"))
      .bind("accountId", accountId);

    if (!countOnly) {
      builder.order("u.upload_date", OrderType.DESC);
    }

    var filter = Filter.and(
      Filter.of("b.active"),
      Filter.of("b.account", ":accountId")
    );

    if (!includePrivate) {
      // the requested account is not our context account - enforce public bookmarks only.
      filter.append(Filter.of("b.public"));
    }

    return builder.where(filter);
  }

  private FetchBuilder _bookmarksForAccount(int accountId, @Nullable Integer before, boolean includePrivate) {
    var builder = _bookmarksForAccount(accountId, includePrivate, false)
      .limit(100);

    if (before != null) {
      builder.where(
        Filter.and(
          builder.getFilter(),
          Filter.of("u.id < :before")
        )
      ).bind("before", before);
    }

    return builder;
  }

  public UploadBookmarkState getBookmarkStateForUpload(int uploadId, @Nullable DBAccount accountContext, Handle handle) {
    var dbBookmarks = readForUpload(uploadId, new BookmarkFetchParams(false, false), handle);
    DBUploadBookmark ourBookmark = null;
    if (accountContext != null) {
      ourBookmark = QueryBuilder.select("*")
        .from("upload_bookmark")
        .where(
          Filter.and(
            Filter.of("upload", ":upload"),
            Filter.of("account", ":account"),
            Filter.of("active")
          )
        )
        .bind("upload", uploadId)
        .bind("account", accountContext.id)
        .toQuery(handle)
        .mapTo(DBUploadBookmark.class)
        .findFirst().orElse(null);
    }

    return new UploadBookmarkState(dbBookmarks.size(), ourBookmark != null, ourBookmark != null && ourBookmark.isPublic);
  }
}
