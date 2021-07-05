package com.mtinge.yuugure.services.database.providers;

import com.mtinge.QueryBuilder.QueryBuilder;
import com.mtinge.QueryBuilder.ops.filter.Filter;
import com.mtinge.QueryBuilder.ops.order.OrderType;
import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.data.http.*;
import com.mtinge.yuugure.data.postgres.DBAccount;
import com.mtinge.yuugure.data.postgres.DBMedia;
import com.mtinge.yuugure.data.postgres.DBMediaMeta;
import com.mtinge.yuugure.data.postgres.DBUpload;
import com.mtinge.yuugure.services.database.Database;
import com.mtinge.yuugure.services.database.UploadFetchParams;
import com.mtinge.yuugure.services.database.props.UploadProps;
import com.mtinge.yuugure.services.database.results.Result;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Query;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UploadProvider extends Provider<DBUpload, UploadProps> {
  private static final Logger logger = LoggerFactory.getLogger(UploadProvider.class);

  @Override
  public Result<DBUpload> create(UploadProps props, Handle handle) {
    requireTransaction(handle);

    return Result.fromValue(
      Database.firstOrNull(
        QueryBuilder.insert("upload")
          .columns("media", "owner", "state", "upload_date")
          .values(":media", ":owner", ":state", "now()")
          .returning("*")
          .bind("media", props.media())
          .bind("owner", props.owner())
          .bind("state", props.state())
          .toQuery(handle),
        DBUpload.Mapper
      )
    );
  }

  @Override
  public DBUpload read(int id, Handle handle) {
    return Database.firstOrNull(
      QueryBuilder.select("*")
        .from("upload")
        .where("id", ":id")
        .bind("id", id)
        .toQuery(handle),
      DBUpload.Mapper
    );
  }

  public DBUpload read(int id, UploadFetchParams params, Handle handle) {
    if (!params.hasFilter()) {
      return read(id, handle);
    }

    return Database.firstOrNull(
      QueryBuilder.select("*")
        .from("upload")
        .where(
          Filter.and(
            Filter.of("id", ":id"),
            Filter.of("(state & :filter)", 0)
          )
        )
        .bind("id", id)
        .bind("filter", params.getFilter())
        .toQuery(handle),
      DBUpload.Mapper
    );
  }

  public List<DBUpload> readForAccount(int id, UploadFetchParams params, Handle handle) {
    return Database.toList(
      _uploadsForAccount(id, params, handle),
      DBUpload.Mapper
    );
  }

  public BulkRenderableUpload readRenderableForAccount(int id, UploadFetchParams params, @Nullable DBAccount context, Handle handle) {
    return makeUploadsRenderable(readForAccount(id, params, handle), context, handle);
  }

  private Query _uploadsForAccount(int accountId, UploadFetchParams params, Handle handle) {
    var builder = QueryBuilder.select("*")
      .from("upload")
      .order("upload_date", OrderType.DESC)
      .bind("owner", accountId);
    var filter = Filter.of("owner", ":owner");

    if (!params.includePrivate() || !params.includeBadFlagged()) {
      long state = 0L;

      state = params.includeBadFlagged() ? state : States.addFlag(state, States.Upload.DMCA, States.Upload.DELETED);
      state = params.includePrivate() ? state : States.addFlag(state, States.Upload.PRIVATE);

      filter = Filter.and(
        filter,
        Filter.of("(state & :state)", 0)
      );
      builder.bind("state", state);
    }

    return builder.where(filter).toQuery(handle);
  }

  @Override
  public Result<DBUpload> update(int id, UploadProps updated, Handle handle) {
    var query = QueryBuilder.update("upload").where("id", ":id").bind("id", id);

    if (updated.media() != null) {
      query.set("media", ":media")
        .bind("media", updated.media());
    }

    if (updated.parent() != null) {
      query.set("parent", ":parent")
        .bind("parent", updated.parent());
    }

    if (updated.owner() != null) {
      query.set("owner", ":owner")
        .bind("owner", updated.owner());
    }

    if (updated.uploadDate() != null) {
      query.set("uploadDate", ":uploadDate")
        .bind("uploadDate", updated.uploadDate());
    }

    if (updated.state() != null) {
      query.set("state", ":state")
        .bind("state", updated.state());
    }

    return Result.fromValue(
      Database.firstOrNull(
        query.toQuery(handle),
        DBUpload.Mapper
      )
    );
  }

  @Override
  public Result<DBUpload> delete(int id, Handle handle) {
    return Result.fromValue(
      Database.firstOrNull(
        QueryBuilder.update("upload")
          .set("state", "(state | :state)")
          .where("id", ":id")
          .returning("*")
          .bind("state", States.Upload.DELETED)
          .toQuery(handle),
        DBUpload.Mapper
      )
    );
  }

  public Result<Integer> deleteForAccount(int accountId, Handle handle) {
    try {
      var deleted = QueryBuilder.update("upload")
        .set("state", "(state | :delete_state)")
        .where("owner", ":owner")
        .bind("delete_state", States.Upload.DELETED)
        .bind("owner", accountId)
        .toUpdate(handle)
        .execute();

      return new Result<>(deleted, true, null);
    } catch (Exception e) {
      logger.error("Failed to delete uploads for account {}", accountId, e);
      return Result.fail(FAIL_SQL);
    }
  }

  /**
   * Extends a {@link DBUpload} and makes it renderable, as per {@link #makeUploadsRenderable(List,
   * DBAccount, Handle) makeUploadsRenderable}.
   *
   * @param upload The upload to extend.
   * @param accountContext The nullable requesting account. Used to check privacy state and
   *   contextual upvote/bookmark state.
   * @param handle The database handle to use.
   *
   * @return The {@link RenderableUpload}.
   */
  public RenderableUpload makeUploadRenderable(DBUpload upload, @Nullable DBAccount accountContext, Handle handle) {
    var media = App.database().media.read(upload.media, handle);
    var meta = App.database().mediaMeta.readForMedia(upload.media, handle);
    var owner = SafeAccount.fromDb(App.database().accounts.read(upload.owner, handle));
    var tags = App.database().tags.readForUpload(upload.id, handle)
      .stream()
      .map(SafeTag::fromDb)
      .collect(Collectors.toList());
    var voteState = App.database().votes.getVoteStateForUpload(upload.id, accountContext, handle);
    var bookmarkState = App.database().bookmarks.getBookmarkStateForUpload(upload.id, accountContext, handle);

    return new RenderableUpload(upload, media, meta, owner, tags, voteState, bookmarkState);
  }

  /**
   * Takes an array of uploads and extends them into a renderable upload. This method caches results
   * so that duplicated media/owners don't hit the database multiple times.
   *
   * @param uploads The uploads to extend.
   * @param handle The existing JDBI handle.
   *
   * @return The list of renderable uploads.
   */
  public BulkRenderableUpload makeUploadsRenderable(List<DBUpload> uploads, @Nullable DBAccount accountContext, Handle handle) {
    var mediaCache = new HashMap<Integer, DBMedia>();
    var metaCache = new HashMap<Integer, DBMediaMeta>();
    var ownerCache = new HashMap<Integer, SafeAccount>();
    var tagCache = new HashMap<Integer, SafeTag>();

    var ret = new LinkedList<ExtendedUpload>();
    for (var upload : uploads) {
      DBMedia media = mediaCache.get(upload.media);
      if (media == null) {
        media = App.database().media.read(upload.media, handle);
        mediaCache.put(upload.media, media);
      }

      // note: we cache on the media ID since that's our indexed association.
      DBMediaMeta meta = metaCache.get(upload.media);
      if (meta == null) {
        meta = App.database().mediaMeta.readForMedia(upload.media, handle);
        metaCache.put(upload.media, meta);
      }

      SafeAccount owner = ownerCache.get(upload.owner);
      if (owner == null) {
        owner = SafeAccount.fromDb(App.database().accounts.read(upload.owner, handle));
        ownerCache.put(upload.owner, owner);
      }

      var tags = App.database().tags.readForUpload(upload.id, handle);
      for (var tag : tags) {
        tagCache.putIfAbsent(tag.id, SafeTag.fromDb(tag));
      }

      // fetch active votes
      var voteState = App.database().votes.getVoteStateForUpload(upload.id, accountContext, handle);

      // fetch public, active upload bookmarks
      var bookmarkState = App.database().bookmarks.getBookmarkStateForUpload(upload.id, accountContext, handle);

      ret.add(new ExtendedUpload(upload, tags.stream().map(t -> t.id).collect(Collectors.toList()), bookmarkState, voteState));
    }

    return new BulkRenderableUpload(ownerCache, tagCache, mediaCache, metaCache, ret);
  }

  public BulkRenderableUpload getIndexUploads(@Nullable DBAccount context, Handle handle) {
    var builder = QueryBuilder.select("*")
      .from("upload")
      .order("upload_date", OrderType.DESC)
      .limit(50);

    long badState = States.compute(States.Upload.DELETED, States.Upload.DMCA);
    long generalState = States.addFlag(badState, States.Upload.PRIVATE);
    if (context != null) {
      // we have an account context, so construct a filter that allows us to see our own private
      // uploads but not other peoples private uploads.
      builder
        .where(
          Filter.or(
            Filter.of("(state & :general_state)", 0),
            Filter.and(
              Filter.of("owner", ":id"),
              Filter.of("(state & :contextual_state)", 0)
            )
          )
        )
        .bind("general_state", generalState)
        .bind("contextual_state", badState)
        .bind("id", context.id);
    } else {
      // we don't have an account context so filter out all filterable uploads.
      builder
        .where(Filter.of("(state & :state)", 0))
        .bind("state", generalState);
    }

    var uploads = builder.toQuery(handle)
      .map(DBUpload.Mapper)
      .collect(Collectors.toList());

    return App.database().uploads.makeUploadsRenderable(uploads, context, handle);
  }

  public BulkRenderableUpload getUploadsForSearch(List<Integer> ids, @Nullable DBAccount context, Handle handle) {
    if (ids.isEmpty()) {
      return new BulkRenderableUpload(Map.of(), Map.of(), Map.of(), Map.of(), List.of());
    }

    var builder = QueryBuilder.select("*")
      .from("upload")
      .order("upload_date", OrderType.DESC);
    var filter = Filter.in("id", ids.stream().map(String::valueOf).toArray(String[]::new));

    long authedState = States.compute(States.Upload.DELETED, States.Upload.DMCA);
    long unauthedState = States.addFlag(authedState, States.Upload.PRIVATE);
    if (context != null) {
      filter = Filter.and(
        Filter.or(
          Filter.of("(state & :unauthedState)", 0),
          Filter.and(
            Filter.of("owner", ":id"),
            Filter.of("(state & :authedState)", 0)
          )
        ),
        filter
      );
      builder
        .bind("unauthedState", unauthedState)
        .bind("authedState", authedState)
        .bind("id", context.id);
    } else {
      filter = Filter.and(
        Filter.of("(state & :state)", 0),
        filter
      );
      builder.bind("state", unauthedState);
    }
    var uploads = builder.where(filter)
      .toQuery(handle)
      .map(DBUpload.Mapper)
      .collect(Collectors.toList());

    return makeUploadsRenderable(uploads, context, handle);
  }
}
