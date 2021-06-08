package com.mtinge.yuugure;

import com.mtinge.yuugure.core.Config;
import com.mtinge.yuugure.services.database.Database;
import com.mtinge.yuugure.services.http.WebServer;
import com.mtinge.yuugure.services.redis.Redis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.nio.file.Path;

public class App {
  private static final Logger logger = LoggerFactory.getLogger(App.class);
  private static final boolean debug = System.getProperty("debug", "false").equalsIgnoreCase("true");

  // utils
  private static Config config;

  // services
  private static WebServer webServer;
  private static Database database;
  private static Redis redis;

  public App() {
    try {
      config = Config.read(Path.of("./config.json").toFile());

      database = new Database();
      redis = new Redis();
      webServer = new WebServer();
    } catch (Exception e) {
      throw new Error("Failed to instantiate services.", e);
    }
  }

  public void run() {
    try {
      redis.init();
      database.init();
      webServer.init();
    } catch (Exception e) {
      throw new Error("Failed to initialize services.", e);
    }

    try {
      redis.start();
      database.start();
      webServer.start();
    } catch (Exception e) {
      throw new Error("Failed to start services.", e);
    }
  }

  public static Config config() {
    return config;
  }

  public static WebServer webServer() {
    return webServer;
  }

  public static Database database() {
    return database;
  }

  public static Redis redis() {
    return redis;
  }

  public static boolean isDebug() {
    return debug;
  }

  public static void main(String[] args) throws Exception {
    var path = Path.of("./config.json");
    var file = path.toFile();
    if (!file.exists()) {
      try (var is = App.class.getClassLoader().getResourceAsStream("defaults.json")) {
        if (is == null) throw new AssertionError("Could not find the default config on classpath");

        try (var fos = new FileOutputStream(file)) {
          is.transferTo(fos);
        }
      }

      System.out.println("Default config has been written to the application directory. Please modify it and run again.");
      System.exit(0);
      return;
    }

    new App().run();
  }
}
