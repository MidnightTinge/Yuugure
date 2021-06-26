package com.mtinge.yuugure;

import com.mtinge.yuugure.core.Config;
import com.mtinge.yuugure.core.TagManager.TagDescriptor;
import com.mtinge.yuugure.core.TagManager.TagManager;
import com.mtinge.yuugure.core.TagManager.TagType;
import com.mtinge.yuugure.services.cli.CLI;
import com.mtinge.yuugure.services.database.Database;
import com.mtinge.yuugure.services.http.WebServer;
import com.mtinge.yuugure.services.messaging.Messaging;
import com.mtinge.yuugure.services.processor.MediaProcessor;
import com.mtinge.yuugure.services.redis.Redis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;

public class App {
  private static final Logger logger = LoggerFactory.getLogger(App.class);
  private static final boolean debug = System.getProperty("debug", "false").equalsIgnoreCase("true");

  // utils
  private static Config config;
  private static TagManager tagManager;

  // services
  private static WebServer webServer;
  private static Database database;
  private static Redis redis;
  private static Messaging messaging;
  private static MediaProcessor mediaProcessor;
  private static CLI cli;

  public App() {
    try {
      config = Config.read(Path.of("./config.json").toFile());
      tagManager = new TagManager();

      database = new Database();
      redis = new Redis();
      messaging = new Messaging();
      mediaProcessor = new MediaProcessor();
      cli = new CLI();
      webServer = new WebServer();
    } catch (Exception e) {
      throw new Error("Failed to instantiate services.", e);
    }
  }

  public void run() {
    try {
      redis.init();
      database.init();
      messaging.init();
      mediaProcessor.init();
      cli.init();
      webServer.init();
    } catch (Exception e) {
      throw new Error("Failed to initialize services.", e);
    }

    try {
      redis.start();
      database.start();
      tagManager.reload();
      messaging.start();
      mediaProcessor.start();
      cli.start();
      webServer.start();
    } catch (Exception e) {
      throw new Error("Failed to start services.", e);
    }
  }

  private void ensureDefaultTags() {
    var tags = List.of(
      // MediaProcessor tags
      new TagDescriptor("video", TagType.META),
      new TagDescriptor("has_audio", TagType.META),

      new TagDescriptor("filesize_tiny", TagType.META),
      new TagDescriptor("filesize_small", TagType.META),
      new TagDescriptor("filesize_medium", TagType.META),
      new TagDescriptor("filesize_large", TagType.META),
      new TagDescriptor("filesize_massive", TagType.META),

      new TagDescriptor("dimensions_tiny", TagType.META),
      new TagDescriptor("dimensions_small", TagType.META),
      new TagDescriptor("dimensions_medium", TagType.META),
      new TagDescriptor("dimensions_large", TagType.META),
      new TagDescriptor("dimensions_massive", TagType.META),

      new TagDescriptor("length_very_short", TagType.META),
      new TagDescriptor("length_short", TagType.META),
      new TagDescriptor("length_medium", TagType.META),
      new TagDescriptor("length_long", TagType.META),
      new TagDescriptor("length_very_log", TagType.META)
    );
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

  public static Messaging messaging() {
    return messaging;
  }

  public static MediaProcessor mediaProcessor() {
    return mediaProcessor;
  }

  public static TagManager tagManager() {
    return tagManager;
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
