package com.mtinge.yuugure.services.database;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.data.postgres.*;
import com.mtinge.yuugure.services.IService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  public DBUpload getUploadById(int id, boolean includeBadFlagged) {
    return jdbi.withHandle(handle -> {
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
    });
  }

  public DBMedia getMediaForUpload(int id) {
    return jdbi.withHandle(handle ->
      handle.createQuery("SELECT * FROM media WHERE id = (SELECT media FROM upload WHERE id = :id)")
        .bind("id", id)
        .map(DBMedia.Mapper)
        .findFirst().orElse(null)
    );
  }

  public DBMedia getMediaById(int id) {
    return jdbi.withHandle(handle ->
      handle.createQuery("SELECT * FROM media WHERE id = :id")
        .bind("id", id)
        .map(DBMedia.Mapper)
        .findFirst().orElse(null)
    );
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
    return jdbi.withHandle(handle ->
      handle.createQuery("SELECT * FROM account WHERE id = :id")
        .bind("id", id)
        .map(DBAccount.Mapper)
        .findFirst().orElse(null)
    );
  }
}
