package com.mtinge.yuugure.services.database;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.data.http.RenderableUpload;
import com.mtinge.yuugure.data.http.SafeAccount;
import com.mtinge.yuugure.data.postgres.*;
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
import java.util.stream.Collectors;

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
    var renderable = makeUploadsRenderable(List.of(upload), handle);

    return renderable.isEmpty() ? null : renderable.get(0);
  }

  /**
   * Shorthand for {@link #makeUploadsRenderable(List, Handle)}
   *
   * @see #makeUploadsRenderable(List, Handle)
   */
  public List<RenderableUpload> makeUploadsRenderable(List<DBUpload> uploads) {
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
  public List<RenderableUpload> makeUploadsRenderable(List<DBUpload> uploads, Handle handle) {
    var mediaCache = new HashMap<Integer, DBMedia>();
    var metaCache = new HashMap<Integer, DBMediaMeta>();
    var ownerCache = new HashMap<Integer, SafeAccount>();

    var ret = new LinkedList<RenderableUpload>();
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

      ret.add(new RenderableUpload(upload, media, meta, owner));
    }

    return ret;
  }

  private Query _uploadsForAccount(int accountId, UploadFetchParams params, Handle handle) {
    Query uploadFetch;
    if (params.includeBadFlagged() && params.includePrivate()) {
      // we want to fetch everything
      uploadFetch = handle.createQuery("SELECT * FROM upload WHERE owner = :owner")
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

  public List<RenderableUpload> getRenderableUploadsForAccount(int accountId, UploadFetchParams params) {
    return jdbi.withHandle(handle -> {
      var uploads = _uploadsForAccount(accountId, params, handle)
        .map(DBUpload.Mapper)
        .collect(Collectors.toList());

      return makeUploadsRenderable(uploads, handle);
    });
  }
}
