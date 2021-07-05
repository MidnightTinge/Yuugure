package com.mtinge.yuugure.services.database;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.services.IService;
import com.mtinge.yuugure.services.database.providers.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Update;
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

  public final AccountProvider accounts = new AccountProvider();
  public final SessionProvider sessions = new SessionProvider();
  public final MediaProvider media = new MediaProvider();
  public final UploadProvider uploads = new UploadProvider();
  public final ProcessingQueueProvider processors = new ProcessingQueueProvider();
  public final MediaMetaProvider mediaMeta = new MediaMetaProvider();
  public final ReportProvider reports = new ReportProvider();
  public final CommentProvider comments = new CommentProvider();
  public final TagProvider tags = new TagProvider();
  public final BookmarkProvider bookmarks = new BookmarkProvider();
  public final VoteProvider votes = new VoteProvider();

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

  public static RowMapper<Boolean> boolMapper(String column) {
    return (r, c) -> r.getBoolean(column);
  }

  public static RowMapper<Integer> intMapper(String column) {
    return (r, c) -> r.getInt(column);
  }
}
