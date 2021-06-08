package com.mtinge.yuugure.services.database;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.services.IService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
}
