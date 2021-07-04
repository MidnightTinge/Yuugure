package com.mtinge.yuugure.services.database;

import com.mtinge.QueryBuilder.QueryBuilder;
import com.mtinge.QueryBuilder.ops.For.ForType;
import com.mtinge.QueryBuilder.ops.filter.Filter;
import com.mtinge.QueryBuilder.ops.join.Join;
import com.mtinge.QueryBuilder.ops.order.OrderType;
import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.core.TagManager.TagCategory;
import com.mtinge.yuugure.core.TagManager.TagDescriptor;
import com.mtinge.yuugure.data.http.*;
import com.mtinge.yuugure.data.postgres.*;
import com.mtinge.yuugure.data.processor.MediaMeta;
import com.mtinge.yuugure.data.processor.ProcessableUpload;
import com.mtinge.yuugure.data.processor.ProcessorResult;
import com.mtinge.yuugure.services.IService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Update;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Accessors(fluent = true)
public class Database implements IService {
  private static final Logger logger = LoggerFactory.getLogger(Database.class);

  @Getter(AccessLevel.NONE)
  private HikariConfig hikariConfig;
  private HikariDataSource dataSource;
  private Jdbi jdbi;

  @Override
  public void init() throws Exception {
    this.hikariConfig = new HikariConfig();
    this.hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
    this.hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
    this.hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    this.hikariConfig.addDataSourceProperty("allowMultiQueries", "true");
    this.hikariConfig.setConnectionInitSql("SET SESSION TIME ZONE 'utc'");

    this.hikariConfig.setJdbcUrl(App.config().postgres.url);
    this.hikariConfig.setUsername(App.config().postgres.username);
    this.hikariConfig.setPassword(App.config().postgres.password);
  }

  @Override
  public void start() throws Exception {
    this.dataSource = new HikariDataSource(this.hikariConfig);
    this.jdbi = Jdbi.create(this.dataSource);
    logger.info("Postgres started");
  }

  public DBUpload getUploadById(int id) {
    return getUploadById(id, new UploadFetchParams(true, true));
  }

  public DBUpload getUploadById(int id, Handle handle) {
    return getUploadById(id, new UploadFetchParams(true, true), handle);
  }

  public DBUpload getUploadById(int id, UploadFetchParams params) {
    return jdbi.withHandle(handle -> getUploadById(id, params, handle));
  }

  public DBUpload getUploadById(int id, UploadFetchParams params, Handle handle) {
    var builder = QueryBuilder.select("*")
      .from("upload")
      .trackBind("id", id);
    var filter = Filter.of("id", ":id");

    if (params.includeBadFlagged() || params.includePrivate()) {
      long state = 0L;
      state = !params.includeBadFlagged() ? States.compute(state, States.Upload.DELETED, States.Upload.DMCA) : state;
      state = !params.includePrivate() ? States.compute(state, States.Upload.PRIVATE) : state;

      filter = Filter.and(
        filter,
        Filter.of("(state & :state)", 0)
      );
      builder.trackBind("state", state);
    }

    return firstOrNull(builder.where(filter).toQuery(handle), DBUpload.Mapper);
  }

  public DBMedia getMediaForUpload(int id) {
    return jdbi.withHandle(handle -> getMediaForUpload(id, handle));
  }

  public DBMedia getMediaForUpload(int id, Handle handle) {
    return firstOrNull(
      QueryBuilder.select("*")
        .from("media")
        .where("id", QueryBuilder.select("media")
          .from("upload")
          .where("id", ":id")
        )
        .trackBind("id", id)
        .toQuery(handle)
      , DBMedia.Mapper);
  }

  public DBMedia getMediaById(int id) {
    return jdbi.withHandle(handle -> getMediaById(id, handle));
  }

  public DBMedia getMediaById(int id, Handle handle) {
    return firstOrNull(
      QueryBuilder.select("*")
        .from("media")
        .where("id", ":id")
        .trackBind("id", id)
        .toQuery(handle),
      DBMedia.Mapper
    );
  }

  public DBMediaMeta getMediaMetaById(int id) {
    return jdbi.withHandle(handle -> getMediaMetaById(id, handle));
  }

  public DBMediaMeta getMediaMetaById(int id, Handle handle) {
    return firstOrNull(
      QueryBuilder.select("*")
        .from("media_meta")
        .where("id", ":id")
        .trackBind("id", id)
        .toQuery(handle),
      DBMediaMeta.Mapper
    );
  }

  public DBMediaMeta getMediaMetaByMedia(int media) {
    return jdbi.withHandle(handle -> getMediaMetaByMedia(media, handle));
  }

  public DBMediaMeta getMediaMetaByMedia(int media, Handle handle) {
    return firstOrNull(
      QueryBuilder.select("*")
        .from("media_meta")
        .where("media", ":media")
        .trackBind("media", media)
        .toQuery(handle),
      DBMediaMeta.Mapper
    );
  }

  public DBReport createReport(DBUpload target, DBAccount reporter, String reason) {
    return _report(ReportTargetType.UPLOAD, target.id, reporter.id, reason);
  }

  public DBReport createReport(DBAccount target, DBAccount reporter, String reason) {
    return _report(ReportTargetType.ACCOUNT, target.id, reporter.id, reason);
  }

  public DBReport createReport(DBComment target, DBAccount reporter, String reason) {
    return _report(ReportTargetType.COMMENT, target.id, reporter.id, reason);
  }

  public List<DBReport> getReportsOnUser(int target) {
    return _getReports(ReportTargetType.ACCOUNT, target);
  }

  public List<DBReport> getReportsOnUpload(int target) {
    return _getReports(ReportTargetType.UPLOAD, target);
  }

  private List<DBReport> _getReports(ReportTargetType targetType, int targetId) {
    return jdbi.withHandle(handle -> _getReports(targetType, targetId, handle));
  }

  private List<DBReport> _getReports(ReportTargetType targetType, int targetId, Handle handle) {
    return toList(
      QueryBuilder.select("*")
        .from("report")
        .where(
          Filter.and(
            Filter.of("target_type", ":type"),
            Filter.of("target_id", ":id")
          )
        )
        .order("timestamp", OrderType.DESC)
        .trackBind("type", targetType.colVal())
        .trackBind("id", targetId)
        .toQuery(handle),
      DBReport.Mapper
    );
  }

  private DBReport _report(ReportTargetType targetType, int targetId, int reporter, String reason) {
    return jdbi.withHandle(handle -> _report(targetType, targetId, reporter, reason, handle));
  }

  private DBReport _report(ReportTargetType targetType, int targetId, int reporter, String reason, Handle handle) {
    return firstOrNull(
      QueryBuilder.insert("report")
        .columns("account", "target_type", "target_id", "content")
        .values(":reporter", ":type", ":id", ":reason")
        .returning("*")
        .trackBind("reporter", reporter)
        .trackBind("type", targetType.colVal())
        .trackBind("id", targetId)
        .trackBind("reason", reason)
        .toQuery(handle),
      DBReport.Mapper
    );
  }

  public DBAccount getAccountById(int id) {
    return jdbi.withHandle(handle -> getAccountById(id, handle));
  }

  public DBAccount getAccountById(int id, Handle handle) {
    return firstOrNull(
      QueryBuilder.select("*")
        .from("account")
        .where("id", ":id")
        .trackBind("id", id)
        .toQuery(handle),
      DBAccount.Mapper
    );
  }

  public DBAccount getAccountById(int id, boolean includeBadFlagged) {
    return jdbi.withHandle(handle -> getAccountById(id, includeBadFlagged, handle));
  }

  public DBAccount getAccountById(int id, boolean includeBadFlagged, Handle handle) {
    var builder = QueryBuilder.select("*")
      .from("account")
      .trackBind("id", id);
    var filter = Filter.of("id", ":id");
    if (!includeBadFlagged) {
      filter = Filter.and(
        filter,
        Filter.of("(state & :state)", 0)
      );
      builder.trackBind("state", States.compute(States.Account.DELETED, States.Account.BANNED, States.Account.DEACTIVATED));
    }
    return firstOrNull(builder.where(filter).toQuery(handle), DBAccount.Mapper);
  }

  public RenderableUpload makeUploadRenderable(DBUpload upload, @Nullable DBAccount accountContext) {
    return jdbi.withHandle(handle -> makeUploadRenderable(upload, accountContext, handle));
  }

  public RenderableUpload makeUploadRenderable(DBUpload upload, @Nullable DBAccount accountContext, Handle handle) {
    var media = getMediaById(upload.media, handle);
    var meta = getMediaMetaByMedia(upload.media, handle);
    var owner = SafeAccount.fromDb(getAccountById(upload.owner, handle));
    var tags = getTagsForUpload(upload.id, handle)
      .stream()
      .map(SafeTag::fromDb)
      .collect(Collectors.toList());
    var voteState = getVoteStateForUpload(upload.id, accountContext, handle);
    var bookmarkState = getBookmarkStateForUpload(upload.id, accountContext, handle);

    return new RenderableUpload(upload, media, meta, owner, tags, voteState, bookmarkState);
  }

  /**
   * Shorthand for {@link #makeUploadsRenderable(List, DBAccount, Handle)}
   *
   * @see #makeUploadsRenderable(List, DBAccount, Handle)
   */
  public BulkRenderableUpload makeUploadsRenderable(List<DBUpload> uploads, @Nullable DBAccount accountContext) {
    return jdbi.withHandle(handle -> makeUploadsRenderable(uploads, accountContext, handle));
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
        media = getMediaById(upload.media, handle);
        mediaCache.put(upload.media, media);
      }

      // note: we cache on the media ID since that's our indexed association.
      DBMediaMeta meta = metaCache.get(upload.media);
      if (meta == null) {
        meta = getMediaMetaByMedia(upload.media, handle);
        metaCache.put(upload.media, meta);
      }

      SafeAccount owner = ownerCache.get(upload.owner);
      if (owner == null) {
        owner = SafeAccount.fromDb(getAccountById(upload.owner, handle));
        ownerCache.put(upload.owner, owner);
      }

      var tags = getTagsForUpload(upload.id, handle);
      for (var tag : tags) {
        tagCache.putIfAbsent(tag.id, SafeTag.fromDb(tag));
      }

      // fetch active votes
      var voteState = getVoteStateForUpload(upload.id, accountContext, handle);

      // fetch public, active upload bookmarks
      var bookmarkState = getBookmarkStateForUpload(upload.id, accountContext, handle);

      ret.add(new ExtendedUpload(upload, tags.stream().map(t -> t.id).collect(Collectors.toList()), bookmarkState, voteState));
    }

    return new BulkRenderableUpload(ownerCache, tagCache, mediaCache, metaCache, ret);
  }

  private Query _uploadsForAccount(int accountId, UploadFetchParams params, Handle handle) {
    var builder = QueryBuilder.select("*")
      .from("upload")
      .order("upload_date", OrderType.DESC)
      .trackBind("owner", accountId);
    var filter = Filter.of("owner", ":owner");

    if (!params.includePrivate() || !params.includeBadFlagged()) {
      long state = 0L;

      state = params.includeBadFlagged() ? state : States.addFlag(state, States.Upload.DMCA, States.Upload.DELETED);
      state = params.includePrivate() ? state : States.addFlag(state, States.Upload.PRIVATE);

      filter = Filter.and(
        filter,
        Filter.of("(state & :state)", 0)
      );
      builder.trackBind("state", state);
    }

    return builder.where(filter).toQuery(handle);
  }

  public List<DBUpload> getUploadsForAccount(int accountId, UploadFetchParams params) {
    return jdbi.withHandle(handle -> getUploadsForAccount(accountId, params, handle));
  }

  public List<DBUpload> getUploadsForAccount(int accountId, UploadFetchParams params, Handle handle) {
    return toList(
      _uploadsForAccount(accountId, params, handle),
      DBUpload.Mapper
    );
  }

  public BulkRenderableUpload getRenderableUploadsForAccount(int accountId, UploadFetchParams params, @Nullable DBAccount accountContext) {
    return jdbi.withHandle(handle -> {
      var uploads = _uploadsForAccount(accountId, params, handle)
        .map(DBUpload.Mapper)
        .collect(Collectors.toList());

      return makeUploadsRenderable(uploads, accountContext, handle);
    });
  }

  public BulkRenderableUpload getRenderableBookmarksForAccount(int accountId, @Nullable DBAccount accountContext) {
    return jdbi.withHandle(handle -> getRenderableBookmarksForAccount(accountId, accountContext, handle));
  }

  public BulkRenderableUpload getRenderableBookmarksForAccount(int accountId, @Nullable DBAccount context, Handle handle) {
    var builder = QueryBuilder.select("u.*")
      .from("upload_bookmark b")
      .join(Join.inner("upload u", "b.upload = u.id"))
      .join(Join.inner("account a", "b.account = a.id"))
      .order("u.upload_date", OrderType.DESC)
      .trackBind("accountId", accountId);
    var filter = Filter.and(
      Filter.of("b.active"),
      Filter.of("b.account", ":accountId")
    );

    if (context == null || context.id != accountId) {
      // the requested account is not our context account - enforce public bookmarks only.
      filter.append(Filter.of("b.public"));
    }

    var uploads = toList(builder.where(filter).toQuery(handle), DBUpload.Mapper);
    return makeUploadsRenderable(uploads, context, handle);
  }

  public void deleteAccount(int account) {
    jdbi.inTransaction(handle -> {
      // lock objects
      handle.execute("SELECT 1 FROM upload WHERE owner = ? FOR UPDATE", account);
      handle.execute("SELECT 1 FROM sessions WHERE account = ? FOR UPDATE", account);
      handle.execute("SELECT 1 FROM account WHERE id = ? FOR UPDATE", account);

      // set delete states
      handle.createUpdate("UPDATE account SET state = (state | :state) WHERE id = :account")
        .bind("account", account)
        .bind("state", States.Account.DELETED)
        .execute();
      handle.createUpdate("UPDATE upload SET state = (state | :state) WHERE owner = :account")
        .bind("account", account)
        .bind("state", States.Upload.DELETED)
        .execute();

      // kill sessions
      handle.createUpdate("DELETE FROM sessions WHERE account = :account")
        .bind("account", account)
        .execute();

      // job done
      return true;
    });
  }

  public int updateAccountEmail(int id, String newEmail, Handle handle) {
    return QueryBuilder.update("account")
      .set("email", "lower(:email)")
      .where("id", ":id")
      .trackBind("email", newEmail)
      .trackBind("id", id)
      .toUpdate(handle)
      .execute();
  }

  public boolean updateAccountEmail(int id, String newEmail) {
    return jdbi.withHandle(handle -> updateAccountEmail(id, newEmail, handle) != 0);
  }

  public int updateAccountPassword(int id, String newPasswordHash, Handle handle) {
    return QueryBuilder.update("account")
      .set("password", ":hash")
      .where("id", ":id")
      .trackBind("id", id)
      .trackBind("hash", newPasswordHash)
      .toUpdate(handle)
      .execute();
  }

  public boolean updateAccountPassword(int id, String newPasswordHash) {
    return jdbi.withHandle(handle -> updateAccountPassword(id, newPasswordHash, handle) != 0);
  }

  public ProcessableUpload dequeueUploadForProcessing() {
    return jdbi.withHandle(this::dequeueUploadForProcessing);
  }

  public ProcessableUpload dequeueUploadForProcessing(Handle handle) {
    if (!handle.isInTransaction()) {
      handle.begin();
    }

    try {
      // select and lock a row
      var id = firstOrNull(
        QueryBuilder.select("id")
          .from("processing_queue")
          .where(Filter.not("dequeued"))
          .order("queued_at", OrderType.ASC)
          .limit(1)
          .withFor(ForType.UPDATE)
          .toQuery(handle),
        (r, c) -> r.getInt("id")
      );

      if (id != null && id > 0) {
        var item = first(
          QueryBuilder.update("processing_queue")
            .set("dequeued", "true")
            .where("id", ":id")
            .returning("*")
            .trackBind("id", id)
            .toQuery(handle),
          DBProcessingQueue.Mapper
        );
        var upload = getUploadById(item.upload, handle);
        var media = getMediaById(upload.media, handle);
        handle.commit();

        return new ProcessableUpload(item, upload, media);
      } else {
        handle.commit();
      }
    } catch (Exception e) {
      logger.error("Failed to dequeue an upload for processing.", e);
      handle.rollback();
    }

    return null;
  }

  public void upsertMediaMeta(MediaMeta meta) {
    jdbi.useHandle(handle -> upsertMediaMeta(meta, handle, true));
  }

  public void upsertMediaMeta(MediaMeta meta, Handle handle, boolean commit) {
    if (!handle.isInTransaction()) {
      handle.begin();
    }

    boolean handled = false;
    try {
      // lock the row
      var id = firstOrNull(
        QueryBuilder.select("id")
          .from("media_meta")
          .where("media", ":media")
          .withFor(ForType.UPDATE)
          .trackBind("media", meta.media())
          .toQuery(handle),
        (r, c) -> r.getInt("id")
      );

      QueryBuilder builder;
      if (id == null || id == 0) {
        builder = QueryBuilder.insert("media_meta")
          .columns("media", "width", "height", "video", "video_duration", "has_audio")
          .values(":media", ":width", ":height", ":video", ":video_duration", ":has_audio")
          .returning("*")
          .trackBind("media", meta.media());
      } else {
        builder = QueryBuilder.update("media_meta")
          .set("width", ":width")
          .set("height", ":height")
          .set("video", ":video")
          .set("video_duration", ":video_duration")
          .set("has_audio", ":has_audio")
          .returning("*")
          .trackBind("id", id);
      }

      var upserted = firstOrNull(
        builder
          .trackBind("width", meta.width())
          .trackBind("height", meta.height())
          .trackBind("video", meta.video())
          .trackBind("video_duration", meta.videoDuration())
          .trackBind("has_audio", meta.hasAudio())
          .toQuery(handle),
        DBMediaMeta.Mapper
      );

      if (upserted != null && commit) {
        handle.commit();
        handled = true;
      }
    } finally {
      if (commit && !handled) {
        handle.rollback();
      }
    }
  }

  public void handleProcessorResult(ProcessorResult result) {
    jdbi.useHandle(handle -> handleProcessorResult(result, handle));
  }

  public void handleProcessorResult(ProcessorResult result, Handle handle) {
    if (!handle.isInTransaction()) {
      handle.begin();
    }

    boolean handled = false;

    try {
      upsertMediaMeta(result.meta(), handle, false);

      // lock the processor_queue row for updates
      handle.execute("SELECT 1 FROM processing_queue WHERE id = ? FOR UPDATE", result.dequeued().queueItem.id);

      QueryBuilder.update("processing_queue")
        .set("finished", "true")
        .set("errored", ":errored")
        .set("error_text", ":error_text")
        .where("id", ":id")
        .trackBind("id", result.dequeued().queueItem.id)
        .trackBind("errored", !result.success())
        .trackBind("error_text", result.message())
        .toUpdate(handle)
        .execute();

      var tds = App.tagManager().ensureAll(result.tags().stream().map(TagDescriptor::parse).collect(Collectors.toList()), false);
      if (!tds.tags.isEmpty()) {
        // We ignore if this was true/false because it'll return false if the tags are the same
        // which can happen on a reprocess.
        addTagsToUpload(result.dequeued().upload.id, tds.tags, handle);

        // Get current tags and filter out system tags (we're overriding with processor result)
        var curTags = toList(
          QueryBuilder.select("t.*")
            .from("upload_tags ut")
            .join(Join.inner("tag t", "ut.tag = t.id"))
            .where("upload", ":upload")
            .trackBind("upload", result.dequeued().upload.id)
            .toQuery(handle),
          DBTag.Mapper
        ).stream()
          .filter(t -> t.category.equalsIgnoreCase(TagCategory.USERLAND.getName()))
          .map(t -> t.id)
          .collect(Collectors.toList());

        // Concat the processor result's tags
        var toSet = Stream
          .concat(tds.tags.stream().map(t -> t.id), curTags.stream())
          .collect(Collectors.toList());

        // Set tags
        App.elastic().setTagsForUpload(result.dequeued().upload.id, toSet);
      } else {
        logger.warn("Failed to create tags for upload {} while handling a processor result. Messages:", result.dequeued().upload.id);
        tds.messages.forEach(logger::warn);
      }

      handle.commit();
      handled = true;
    } finally {
      if (!handled) {
        handle.rollback();
      }
    }
  }

  public DBComment getCommentById(int id, boolean includeBadFlagged) {
    return jdbi.withHandle(handle -> getCommentById(id, includeBadFlagged, handle));
  }

  public DBComment getCommentById(int id, boolean includeBadFlagged, Handle handle) {
    var builder = QueryBuilder.select("*").from("comment");
    var filter = Filter.of("id", ":id");

    if (!includeBadFlagged) {
      filter = Filter.and(
        filter,
        Filter.of("active")
      );
    }

    return firstOrNull(
      builder
        .where(filter)
        .trackBind("id", id)
        .toQuery(handle),
      DBComment.Mapper
    );
  }

  public List<DBComment> getCommentsForUpload(int id, boolean includeBadFlagged) {
    return jdbi.withHandle(handle -> this.getCommentsForUpload(id, includeBadFlagged, handle));
  }

  public List<DBComment> getCommentsForUpload(int id, boolean includeBadFlagged, Handle handle) {
    var query = QueryBuilder.select("*")
      .from("comment")
      .order("timestamp", OrderType.DESC)
      .trackBind("id", id);
    var filter = Filter.of("target_id", ":id");

    if (!includeBadFlagged) {
      filter = Filter.and(
        filter,
        Filter.of("active")
      );
    }

    return toList(
      query.where(filter).toQuery(handle),
      DBComment.Mapper
    );
  }

  public DBComment createComment(DBUpload upload, DBAccount account, String raw, String rendered) {
    return jdbi.withHandle(handle -> createComment(upload, account, raw, rendered, handle));
  }

  public DBComment createComment(DBUpload upload, DBAccount account, String raw, String rendered, Handle handle) {
    return firstOrNull(
      QueryBuilder.insert("comment")
        .columns("target_type", "target_id", "account", "content_raw", "content_rendered")
        .values(":type", ":id", ":account", ":raw", ":rendered")
        .returning("*")
        .trackBind("type", DBComment.TYPE_UPLOAD)
        .trackBind("id", upload.id)
        .trackBind("account", account.id)
        .trackBind("raw", raw)
        .trackBind("rendered", rendered)
        .toQuery(handle),
      DBComment.Mapper
    );
  }

  public RenderableComment makeCommentRenderable(DBComment comment) {
    return jdbi.withHandle(handle -> makeCommentRenderable(comment, handle));
  }

  public RenderableComment makeCommentRenderable(DBComment comment, Handle handle) {
    var renderable = makeCommentsRenderable(List.of(comment), handle);

    return renderable != null ? renderable.get(0) : null;
  }

  public List<RenderableComment> makeCommentsRenderable(List<DBComment> comments) {
    return jdbi.withHandle(handle -> makeCommentsRenderable(comments, handle));
  }

  public List<RenderableComment> makeCommentsRenderable(List<DBComment> comments, Handle handle) {
    var accountCache = new HashMap<Integer, SafeAccount>();

    var ret = new LinkedList<RenderableComment>();
    for (var comment : comments) {
      var account = accountCache.get(comment.account);
      if (account == null) {
        account = SafeAccount.fromDb(getAccountById(comment.account, handle));
        accountCache.put(comment.account, account);
      }

      ret.add(new RenderableComment(comment.id, comment.timestamp, account, comment.contentRaw, comment.contentRendered));
    }

    return ret;
  }

  public List<RenderableComment> getRenderableCommentsForUpload(int id, boolean includeBadFlagged) {
    return jdbi.inTransaction(handle -> getRenderableCommentsForUpload(id, includeBadFlagged, handle));
  }

  public List<RenderableComment> getRenderableCommentsForUpload(int id, boolean includeBadFlagged, Handle handle) {
    return makeCommentsRenderable(getCommentsForUpload(id, includeBadFlagged, handle), handle);
  }

  public BulkRenderableUpload getIndexUploads(@Nullable DBAccount context) {
    return jdbi.withHandle(handle -> getIndexUploads(context, handle));
  }

  public BulkRenderableUpload getIndexUploads(@Nullable DBAccount context, Handle handle) {
    var builder = QueryBuilder.select("*")
      .from("upload")
      .order("upload_date", OrderType.DESC)
      .limit(50);

    long badState = States.compute(States.Upload.DELETED, States.Upload.DMCA);
    long generalState = States.addFlag(badState, States.Upload.PRIVATE);
    if (context != null) {
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
        .trackBind("general_state", generalState)
        .trackBind("contextual_state", badState)
        .trackBind("id", context.id);
    } else {
      builder
        .where(Filter.of("(state & :state)", 0))
        .trackBind("state", generalState);
    }

    var uploads = builder.toQuery(handle)
      .map(DBUpload.Mapper)
      .collect(Collectors.toList());

    return makeUploadsRenderable(uploads, context, handle);
  }

  public boolean addTagsToUpload(int id, List<DBTag> tags) {
    return jdbi.withHandle(handle -> addTagsToUpload(id, tags, handle));
  }

  public boolean addTagsToUpload(int id, List<DBTag> tags, Handle handle) {
    var batch = handle.prepareBatch("INSERT INTO upload_tags (upload, tag) VALUES (:upload, :tag) ON CONFLICT DO NOTHING");
    for (var tag : tags) {
      batch.bind("upload", id).bind("tag", tag.id).add();
    }

    var counts = batch.execute();
    for (var count : counts) {
      if (count > 0) {
        return true;
      }
    }

    return false;
  }

  public boolean removeTagsFromUpload(int id, List<DBTag> tags) {
    return jdbi.withHandle(handle -> removeTagsFromUpload(id, tags, handle));
  }

  public boolean removeTagsFromUpload(int id, List<DBTag> tags, Handle handle) {
    var batch = handle.prepareBatch("DELETE FROM upload_tags WHERE upload = :upload AND tag = :tag");
    for (var tag : tags) {
      batch.bind("upload", id).bind("tag", tag.id);
    }

    var counts = batch.execute();
    for (var count : counts) {
      if (count > 0) {
        return true;
      }
    }

    return false;
  }

  public DBTag getTagById(int id) {
    return jdbi.withHandle(handle -> getTagById(id, handle));
  }

  public DBTag getTagById(int id, Handle handle) {
    return QueryBuilder.select("*")
      .from("tag")
      .where("id", ":id")
      .trackBind("id", id)
      .toQuery(handle)
      .map(DBTag.Mapper)
      .findFirst().orElse(null);
  }

  public List<DBTag> getTagsById(List<Integer> ids) {
    return jdbi.withHandle(handle -> getTagsById(ids, handle));
  }

  public List<DBTag> getTagsById(List<Integer> ids, Handle handle) {
    return QueryBuilder.select("*")
      .from("tag")
      .where(Filter.in("id", ids.stream().map(String::valueOf).toArray(String[]::new)))
      .toQuery(handle)
      .map(DBTag.Mapper)
      .collect(Collectors.toList());
  }

  public List<DBTag> getTagsForUpload(int id) {
    return jdbi.withHandle(handle -> getTagsForUpload(id, handle));
  }

  public List<DBTag> getTagsForUpload(int id, Handle handle) {
    return handle.createQuery("SELECT t.* FROM upload_tags ut INNER JOIN tag t ON t.id = ut.tag WHERE ut.upload = :id")
      .bind("id", id)
      .map(DBTag.Mapper)
      .collect(Collectors.toList());
  }

  public BulkRenderableUpload getUploadsForSearch(List<Integer> ids, DBAccount context) {
    return jdbi.inTransaction(handle -> getUploadsForSearch(ids, context, handle));
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
        .trackBind("unauthedState", unauthedState)
        .trackBind("authedState", authedState)
        .trackBind("id", context.id);
    } else {
      filter = Filter.and(
        Filter.of("(state & :state)", 0),
        filter
      );
      builder.trackBind("state", unauthedState);
    }
    var uploads = builder.where(filter)
      .toQuery(handle)
      .map(DBUpload.Mapper)
      .collect(Collectors.toList());

    return makeUploadsRenderable(uploads, context, handle);
  }

  public List<DBUploadVote> getVotesForUpload(int uploadId, boolean includeInactive) {
    return jdbi.withHandle(handle -> getVotesForUpload(uploadId, includeInactive, handle));
  }

  public List<DBUploadVote> getVotesForUpload(int uploadId, boolean includeInactive, Handle handle) {
    var builder = QueryBuilder.select("*")
      .from("upload_vote")
      .trackBind("upload", uploadId);

    var filter = Filter.of("upload", ":upload");

    if (!includeInactive) {
      filter = Filter.and(
        filter,
        Filter.of("active")
      );
    }

    return builder
      .where(filter)
      .toQuery(handle)
      .map(DBUploadVote.Mapper)
      .collect(Collectors.toList());
  }

  public UploadVoteState getVoteStateForUpload(int uploadId, @Nullable DBAccount accountContext, Handle handle) {
    var dbVotes = getVotesForUpload(uploadId, false);

    DBUploadVote ourVote = null;
    int totalUpvotes = 0;
    int totalDownvotes = 0;

    for (var vote : dbVotes) {
      if (vote.isUp) {
        totalUpvotes++;
      } else {
        totalDownvotes++;
      }

      if (accountContext != null && ourVote == null) {
        if (vote.account == accountContext.id) {
          ourVote = vote;
        }
      }
    }

    return new UploadVoteState(totalUpvotes, totalDownvotes, ourVote != null, ourVote != null && ourVote.isUp);
  }

  public List<DBUploadBookmark> getBookmarksForUpload(int uploadId, BookmarkFetchParams fetchParams) {
    return jdbi.withHandle(handle -> getBookmarksForUpload(uploadId, fetchParams, handle));
  }

  public List<DBUploadBookmark> getBookmarksForUpload(int uploadId, BookmarkFetchParams fetchParams, Handle handle) {
    var builder = QueryBuilder.select("*")
      .from("upload_bookmark")
      .trackBind("upload", uploadId);
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
      .map(DBUploadBookmark.Mapper)
      .collect(Collectors.toList());
  }

  public UploadBookmarkState getBookmarkStateForUpload(int uploadId, @Nullable DBAccount accountContext, Handle handle) {
    var dbBookmarks = getBookmarksForUpload(uploadId, new BookmarkFetchParams(false, false));
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
        .trackBind("upload", uploadId)
        .trackBind("account", accountContext.id)
        .toQuery(handle)
        .map(DBUploadBookmark.Mapper)
        .findFirst().orElse(null);
    }

    return new UploadBookmarkState(dbBookmarks.size(), ourBookmark != null, ourBookmark != null && ourBookmark.isPublic);
  }

  public static <T> T first(Query query, RowMapper<T> mapper) {
    return query.map(mapper).first();
  }

  public static <T> T firstOrNull(Query query, RowMapper<T> mapper) {
    return query.map(mapper).findFirst().orElse(null);
  }

  public static <T> List<T> toList(Query query, RowMapper<T> mapper) {
    return query.map(mapper).collect(Collectors.toList());
  }

  public static boolean updated(Update query) {
    return query.execute() > 0;
  }
}
