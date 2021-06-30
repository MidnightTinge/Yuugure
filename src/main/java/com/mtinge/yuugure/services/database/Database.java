package com.mtinge.yuugure.services.database;

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
import org.jdbi.v3.core.statement.Query;
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
    return getUploadById(id, true);
  }

  public DBUpload getUploadById(int id, Handle handle) {
    return getUploadById(id, true, handle);
  }

  public DBUpload getUploadById(int id, boolean includeBadFlagged) {
    return jdbi.withHandle(handle -> getUploadById(id, includeBadFlagged, handle));
  }

  public DBUpload getUploadById(int id, boolean includeBadFlagged, Handle handle) {
    Query query;

    if (includeBadFlagged) {
      query = handle.createQuery("SELECT * FROM upload WHERE id = :id")
        .bind("id", id);
    } else {
      query = handle.createQuery("SELECT * FROM upload WHERE id = :id AND (state & :state) = 0")
        .bind("id", id)
        .bind("state", States.compute(States.Upload.DELETED, States.Upload.DMCA));
    }

    return query
      .map(DBUpload.Mapper)
      .findFirst().orElse(null);
  }

  public DBMedia getMediaForUpload(int id) {
    return jdbi.withHandle(handle -> getMediaForUpload(id, handle));
  }

  public DBMedia getMediaForUpload(int id, Handle handle) {
    return handle.createQuery("SELECT * FROM media WHERE id = (SELECT media FROM upload WHERE id = :id)")
      .bind("id", id)
      .map(DBMedia.Mapper)
      .findFirst().orElse(null);
  }

  public DBMedia getMediaById(int id) {
    return jdbi.withHandle(handle -> getMediaById(id, handle));
  }

  public DBMedia getMediaById(int id, Handle handle) {
    return handle.createQuery("SELECT * FROM media WHERE id = :id")
      .bind("id", id)
      .map(DBMedia.Mapper)
      .findFirst().orElse(null);
  }

  public DBMediaMeta getMediaMetaById(int id) {
    return jdbi.withHandle(handle -> getMediaMetaById(id, handle));
  }

  public DBMediaMeta getMediaMetaById(int id, Handle handle) {
    return handle.createQuery("SELECT * FROM media_meta WHERE id = :id")
      .bind("id", id)
      .map(DBMediaMeta.Mapper)
      .findFirst().orElse(null);
  }

  public DBMediaMeta getMediaMetaByMedia(int media) {
    return jdbi.withHandle(handle -> getMediaMetaByMedia(media, handle));
  }

  public DBMediaMeta getMediaMetaByMedia(int media, Handle handle) {
    return handle.createQuery("SELECT * FROM media_meta WHERE media = :media")
      .bind("media", media)
      .map(DBMediaMeta.Mapper)
      .findFirst().orElse(null);
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
    return jdbi.withHandle(handle ->
      handle.createQuery("SELECT * FROM report WHERE target_type = :target_type AND target_id = :target_id ORDER BY timestamp DESC")
        .bind("target_type", targetType.colVal())
        .bind("target_id", targetId)
        .map(DBReport.Mapper)
        .collect(Collectors.toList())
    );
  }

  private DBReport _report(ReportTargetType targetType, int targetId, int reporter, String reason) {
    return jdbi.withHandle(handle ->
      handle.createQuery("INSERT INTO report (account, target_type, target_id, content) VALUES (:reporter, :target_type, :target_id, :reason) RETURNING *")
        .bind("target_type", targetType.colVal())
        .bind("reporter", reporter)
        .bind("target_id", targetId)
        .bind("reason", reason)
        .map(DBReport.Mapper)
        .findFirst().orElse(null)
    );
  }

  public DBAccount getAccountById(int id) {
    return jdbi.withHandle(handle -> getAccountById(id, handle));
  }

  public DBAccount getAccountById(int id, Handle handle) {
    return handle.createQuery("SELECT * FROM account WHERE id = :id")
      .bind("id", id)
      .map(DBAccount.Mapper)
      .findFirst().orElse(null);
  }

  public DBAccount getAccountById(int id, boolean includeBadFlagged) {
    return jdbi.withHandle(handle -> getAccountById(id, includeBadFlagged, handle));
  }

  public DBAccount getAccountById(int id, boolean includeBadFlagged, Handle handle) {
    Query query;

    if (includeBadFlagged) {
      query = handle.createQuery("SELECT * FROM account WHERE id = :id")
        .bind("id", id);
    } else {
      query = handle.createQuery("SELECT * FROM account WHERE id = :id AND (state & :state) = 0")
        .bind("id", id)
        .bind("state", States.compute(States.Account.DELETED, States.Account.BANNED, States.Account.DEACTIVATED));
    }

    return query
      .map(DBAccount.Mapper)
      .findFirst().orElse(null);
  }

  public RenderableUpload makeUploadRenderable(DBUpload upload) {
    return jdbi.withHandle(handle -> makeUploadRenderable(upload, handle));
  }

  public RenderableUpload makeUploadRenderable(DBUpload upload, Handle handle) {
    var media = getMediaById(upload.media, handle);
    var meta = getMediaMetaByMedia(upload.media, handle);
    var owner = SafeAccount.fromDb(getAccountById(upload.owner, handle));
    var tags = getTagsForUpload(upload.id, handle)
      .stream()
      .map(SafeTag::fromDb)
      .collect(Collectors.toList());

    return new RenderableUpload(upload, media, meta, owner, tags);
  }

  /**
   * Shorthand for {@link #makeUploadsRenderable(List, Handle)}
   *
   * @see #makeUploadsRenderable(List, Handle)
   */
  public BulkRenderableUpload makeUploadsRenderable(List<DBUpload> uploads) {
    return jdbi.withHandle(handle -> makeUploadsRenderable(uploads, handle));
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
  public BulkRenderableUpload makeUploadsRenderable(List<DBUpload> uploads, Handle handle) {
    var mediaCache = new HashMap<Integer, DBMedia>();
    var metaCache = new HashMap<Integer, DBMediaMeta>();
    var ownerCache = new HashMap<Integer, SafeAccount>();
    var tagCache = new HashMap<Integer, SafeTag>();

    var ret = new LinkedList<TaggedUpload>();
    for (var upload : uploads) {
      DBMedia media = mediaCache.get(upload.media);
      if (media == null) {
        media = getMediaById(upload.media, handle);
        mediaCache.put(upload.media, media);
      }

      // note: we cache on the media ID since that's our indexed association.
      DBMediaMeta meta = metaCache.get(upload.media);
      if (meta == null) {
        meta = getMediaMetaByMedia(upload.media);
        metaCache.put(upload.media, meta);
      }

      SafeAccount owner = ownerCache.get(upload.owner);
      if (owner == null) {
        owner = SafeAccount.fromDb(getAccountById(upload.owner, handle));
        ownerCache.put(upload.owner, owner);
      }

      var tags = getTagsForUpload(upload.id);
      for (var tag : tags) {
        tagCache.putIfAbsent(tag.id, SafeTag.fromDb(tag));
      }

      ret.add(new TaggedUpload(upload, tags.stream().map(t -> t.id).collect(Collectors.toList())));
    }

    return new BulkRenderableUpload(ownerCache, tagCache, mediaCache, metaCache, ret);
  }

  private Query _uploadsForAccount(int accountId, UploadFetchParams params, Handle handle) {
    Query uploadFetch;
    if (params.includeBadFlagged() && params.includePrivate()) {
      // we want to fetch everything
      uploadFetch = handle.createQuery("SELECT * FROM upload WHERE owner = :owner ORDER BY upload_date DESC")
        .bind("owner", accountId);
    } else {
      // we want to do some state filtering
      long stateFilter = 0L;
      if (!params.includeBadFlagged()) {
        stateFilter = States.addFlag(stateFilter, States.compute(States.Upload.DMCA, States.Upload.DELETED));
      }
      if (!params.includePrivate()) {
        stateFilter = States.addFlag(stateFilter, States.Upload.PRIVATE);
      }
      uploadFetch = handle.createQuery("SELECT * FROM upload WHERE owner = :owner AND (state & :state) = 0")
        .bind("owner", accountId)
        .bind("state", stateFilter);
    }

    return uploadFetch;
  }

  public List<DBUpload> getUploadsForAccount(int accountId, UploadFetchParams params) {
    return jdbi.withHandle(handle ->
      _uploadsForAccount(accountId, params, handle)
        .map(DBUpload.Mapper)
        .collect(Collectors.toList())
    );
  }

  public BulkRenderableUpload getRenderableUploadsForAccount(int accountId, UploadFetchParams params) {
    return jdbi.withHandle(handle -> {
      var uploads = _uploadsForAccount(accountId, params, handle)
        .map(DBUpload.Mapper)
        .collect(Collectors.toList());

      return makeUploadsRenderable(uploads, handle);
    });
  }

  public void deleteAccount(int account) {
    jdbi.inTransaction(handle -> {
      // lock objects
      handle.createQuery("SELECT * FROM upload WHERE owner = :owner FOR UPDATE")
        .bind("owner", account)
        .execute((statementSupplier, ctx) -> null);
      handle.createUpdate("SELECT * FROM sessions WHERE account = :owner FOR UPDATE")
        .bind("owner", account)
        .execute();
      handle.createUpdate("SELECT * FROM account WHERE id = :account FOR UPDATE")
        .bind("account", account)
        .execute();

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
    return handle.createUpdate("UPDATE account SET email = lower(:email) WHERE id = :id")
      .bind("id", id)
      .bind("email", newEmail)
      .execute();
  }

  public boolean updateAccountEmail(int id, String newEmail) {
    return jdbi.withHandle(handle -> updateAccountEmail(id, newEmail, handle) != 0);
  }

  public int updateAccountPassword(int id, String newPasswordHash, Handle handle) {
    return handle.createUpdate("UPDATE account SET password = :hash WHERE id = :id")
      .bind("id", id)
      .bind("hash", newPasswordHash)
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
      var id = handle.createQuery("SELECT id FROM processing_queue WHERE NOT dequeued ORDER BY queued_at LIMIT 1 FOR UPDATE")
        .map((r, c) -> r.getInt("id"))
        .findFirst().orElse(null);

      if (id != null) {
        var item = handle.createQuery("UPDATE processing_queue SET dequeued = true WHERE id = :id RETURNING *")
          .bind("id", id)
          .map(DBProcessingQueue.Mapper)
          .first();
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
      var id = handle.createQuery("SELECT id FROM media_meta WHERE media = :media FOR UPDATE")
        .bind("media", meta.media())
        .map((r, __) -> r.getInt("id"))
        .findFirst().orElse(null);

      Query query;
      if (id == null) {
        query = handle.createQuery("INSERT INTO media_meta (media, width, height, video, video_duration, has_audio) VALUES (:media, :width, :height, :video, :video_duration, :has_audio) RETURNING *")
          .bind("media", meta.media());
      } else {
        query = handle.createQuery("UPDATE media_meta SET width = :width, height = :height, video = :video, video_duration = :video_duration, has_audio = :has_audio WHERE id = :id RETURNING *")
          .bind("id", id);
      }

      var upserted = query
        .bind("width", meta.width())
        .bind("height", meta.height())
        .bind("video", meta.video())
        .bind("video_duration", meta.videoDuration())
        .bind("has_audio", meta.hasAudio())
        .map(DBMediaMeta.Mapper)
        .findFirst().orElse(null);

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
      handle.createQuery("SELECT 1 FROM processing_queue WHERE id = :id FOR UPDATE")
        .bind("id", result.dequeued().queueItem.id);

      handle.createUpdate("UPDATE processing_queue SET finished = true, errored = :errored, error_text = :error_text WHERE id = :id")
        .bind("id", result.dequeued().queueItem.id)
        .bind("errored", !result.success())
        .bind("error_text", result.message())
        .execute();

      var tds = App.tagManager().ensureAll(result.tags().stream().map(TagDescriptor::parse).collect(Collectors.toList()), false);
      if (!tds.tags.isEmpty()) {
        // We ignore if this was true/false because it'll return false if the tags are the same
        // which can happen on a reprocess.
        addTagsToUpload(result.dequeued().upload.id, tds.tags, handle);

        // Get current tags and filter out system tags (we're overriding with processor result)
        var curTags = handle.createQuery("SELECT t.* FROM upload_tags ut INNER JOIN tag t on ut.tag = t.id WHERE upload = :upload")
          .bind("upload", result.dequeued().upload.id)
          .map(DBTag.Mapper)
          .stream()
          .filter(t -> t.category.equalsIgnoreCase(TagCategory.USERLAND.getName()))
          .map(t -> t.id)
          .collect(Collectors.toList());

        // Concat the processor result's tags
        var toSet = Stream.concat(tds.tags.stream().map(t -> t.id), curTags.stream())
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
    Query query;
    if (includeBadFlagged) {
      query = handle.createQuery("SELECT * FROM comment WHERE id = :id");
    } else {
      query = handle.createQuery("SELECT * FROM comment WHERE id = :id AND active");
    }

    return query
      .bind("id", id)
      .map(DBComment.Mapper)
      .findFirst().orElse(null);
  }

  public List<DBComment> getCommentsForUpload(int id, boolean includeBadFlagged) {
    return jdbi.withHandle(handle -> this.getCommentsForUpload(id, includeBadFlagged, handle));
  }

  public List<DBComment> getCommentsForUpload(int id, boolean includeBadFlagged, Handle handle) {
    Query query;
    if (includeBadFlagged) {
      query = handle.createQuery("SELECT * FROM comment WHERE target_type = :type AND target_id = :id ORDER BY timestamp DESC");
    } else {
      query = handle.createQuery("SELECT * FROM comment WHERE target_type = :type AND target_id = :id AND active ORDER BY timestamp DESC");
    }

    return query
      .bind("type", DBComment.TYPE_UPLOAD)
      .bind("id", id)
      .map(DBComment.Mapper).collect(Collectors.toList());
  }

  public DBComment createComment(DBUpload upload, DBAccount account, String raw, String rendered) {
    return jdbi.withHandle(handle -> createComment(upload, account, raw, rendered, handle));
  }

  public DBComment createComment(DBUpload upload, DBAccount account, String raw, String rendered, Handle handle) {
    return handle.createQuery("INSERT INTO comment (target_type, target_id, account, content_raw, content_rendered) VALUES (:type, :id, :account, :raw, :rendered) RETURNING *")
      .bind("type", DBComment.TYPE_UPLOAD)
      .bind("id", upload.id)
      .bind("account", account.id)
      .bind("raw", raw)
      .bind("rendered", rendered)
      .map(DBComment.Mapper)
      .findFirst().orElse(null);
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

  public BulkRenderableUpload getIndexUploads(DBAccount context) {
    return jdbi.withHandle(handle -> getIndexUploads(context, handle));
  }

  public BulkRenderableUpload getIndexUploads(DBAccount context, Handle handle) {
    Query uploadsQuery;
    if (context != null) {
      long badState = States.compute(States.Upload.DELETED, States.Upload.DMCA);
      uploadsQuery = handle.createQuery("SELECT * FROM upload WHERE (state & :general_state) = 0 OR (owner = :id AND (state & :contextual_state) = 0) ORDER BY upload_date DESC LIMIT 50")
        .bind("general_state", States.compute(badState, States.Upload.PRIVATE))
        .bind("contextual_state", badState)
        .bind("id", context.id);
    } else {
      uploadsQuery = handle.createQuery("SELECT * FROM upload WHERE (state & :state) = 0 ORDER BY upload_date DESC LIMIT 50")
        .bind("state", States.compute(States.Upload.DELETED, States.Upload.DMCA, States.Upload.PRIVATE));
    }

    var uploads = uploadsQuery
      .map(DBUpload.Mapper)
      .collect(Collectors.toList());

    return makeUploadsRenderable(uploads, handle);
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
    return jdbi.withHandle(handle -> getTagById(id));
  }

  public DBTag getTagById(int id, Handle handle) {
    return handle.createQuery("SELECT * FROM tag WHERE id = :id")
      .bind("id", id)
      .map(DBTag.Mapper)
      .findFirst().orElse(null);
  }

  public List<DBTag> getTagsById(List<Integer> ids) {
    return jdbi.withHandle(handle -> getTagsById(ids, handle));
  }

  public List<DBTag> getTagsById(List<Integer> ids, Handle handle) {
    var qstr = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    return handle.createQuery("SELECT * FROM tag WHERE id IN (" + qstr + ")")
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

  public BulkRenderableUpload getUploadsForSearch(List<Integer> ids, DBAccount context, Handle handle) {
    if (ids.isEmpty()) {
      return new BulkRenderableUpload(Map.of(), Map.of(), Map.of(), Map.of(), List.of());
    }

    var qstr = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    Query uploadsQuery;
    if (context != null) {
      long badState = States.compute(States.Upload.DELETED, States.Upload.DMCA);
      uploadsQuery = handle.createQuery("SELECT * FROM upload WHERE ((state & :general_state) = 0 OR (owner = :id AND (state & :contextual_state) = 0)) AND id IN (" + qstr + ") ORDER BY upload_date DESC LIMIT 50")
        .bind("general_state", States.compute(badState, States.Upload.PRIVATE))
        .bind("contextual_state", badState)
        .bind("id", context.id);
    } else {
      uploadsQuery = handle.createQuery("SELECT * FROM upload WHERE ((state & :state) = 0) AND id IN (" + qstr + ") ORDER BY upload_date DESC LIMIT 50")
        .bind("state", States.compute(States.Upload.DELETED, States.Upload.DMCA, States.Upload.PRIVATE));
    }

    var uploads = uploadsQuery
      .map(DBUpload.Mapper)
      .collect(Collectors.toList());

    return makeUploadsRenderable(uploads, handle);
  }

}
