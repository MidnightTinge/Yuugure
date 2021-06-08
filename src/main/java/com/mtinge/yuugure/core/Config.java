package com.mtinge.yuugure.core;


import com.mtinge.yuugure.core.adapters.DurationAdapter;
import com.squareup.moshi.Json;
import com.squareup.moshi.Moshi;
import com.zaxxer.hikari.HikariConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import okio.Okio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.util.List;

@AllArgsConstructor
public final class Config {
  private static final Logger logger = LoggerFactory.getLogger(Config.class);

  public static Config read(File configFile) {
    if (!configFile.exists()) throw new IllegalArgumentException("Config file does not exist");
    var moshi = new Moshi.Builder()
      .add(Duration.class, new DurationAdapter())
      .build();

    try {
      return moshi.adapter(Config.class).fromJson(Okio.buffer(Okio.source(configFile)));
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid config provided, failed to read: " + e.getMessage(), e);
    }
  }


  ///


  public final HTTP http;
  public final Postgres postgres;
  public final Redis redis;


  private Config() {
    this.http = new HTTP();
    this.postgres = new Postgres();
    this.redis = new Redis();
  }

  @AllArgsConstructor
  public static final class HTTP {
    public final String host;
    public final String realIPHeader;
    public final int port;
    public final Auth auth;

    public HTTP() {
      this.host = "127.0.0.1";
      this.port = 64174;
      this.realIPHeader = null;

      this.auth = new Auth();
    }

    @AllArgsConstructor
    public static final class Auth {
      @Json(name = "cookie_name")
      public final String cookieName;
      @Json(name = "session_expires")
      public final Duration sessionExpires;
      public final String domain;
      public final Boolean secure;

      public Auth() {
        this.cookieName = "esess";
        this.sessionExpires = Duration.ofDays(2);
        this.domain = "";
        this.secure = true;
      }
    }
  }

  @AllArgsConstructor
  public static final class Redis {
    public final String url;

    public Redis() {
      this.url = "redis://localhost:6379/1";
    }
  }

  @AllArgsConstructor
  public static final class Postgres {
    public final String url;
    public final String username;
    public final String password;

    public Postgres() {
      this.url = "jdbc:postgresql://127.0.0.1:5432/yuugure";
      this.username = "yuugure";
      this.password = "password";
    }
  }

}
