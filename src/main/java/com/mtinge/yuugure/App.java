package com.mtinge.yuugure;

import com.mtinge.yuugure.core.Config;
import com.mtinge.yuugure.core.TagManager.TagCategory;
import com.mtinge.yuugure.core.TagManager.TagDescriptor;
import com.mtinge.yuugure.core.TagManager.TagManager;
import com.mtinge.yuugure.services.cli.CLI;
import com.mtinge.yuugure.services.database.Database;
import com.mtinge.yuugure.services.elastic.Elastic;
import com.mtinge.yuugure.services.http.WebServer;
import com.mtinge.yuugure.services.messaging.Messaging;
import com.mtinge.yuugure.services.processor.MediaProcessor;
import com.mtinge.yuugure.services.redis.Redis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
  private static Elastic elastic;
  private static Messaging messaging;
  private static MediaProcessor mediaProcessor;
  private static CLI cli;

  public App() {
    try {
      config = Config.read(Path.of("./config.json").toFile());
      tagManager = new TagManager();

      database = new Database();
      redis = new Redis();
      elastic = new Elastic();
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
      elastic.init();
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
      elastic.start();
      messaging.start();

      tagManager.reload();
      ensureDefaultTags();

      mediaProcessor.start();
      cli.start();
      webServer.start();
    } catch (Exception e) {
      throw new Error("Failed to start services.", e);
    }
  }

  private void ensureDefaultTags() {
    // extracted for associations
    var fileSizeMassive = new TagDescriptor(TagCategory.FILESIZE, "massive");
    var dimensionsMassive = new TagDescriptor(TagCategory.DIMENSIONS, "massive");
    var metaHasAudio = new TagDescriptor(TagCategory.META, "has_audio");

    var ratingSafe = new TagDescriptor(TagCategory.RATING, "safe");
    var ratingQuestionable = new TagDescriptor(TagCategory.RATING, "questionable");
    var ratingExplicit = new TagDescriptor(TagCategory.RATING, "explicit");

    var tags = List.of(
      new TagDescriptor(TagCategory.FILESIZE, "tiny"),
      new TagDescriptor(TagCategory.FILESIZE, "small"),
      new TagDescriptor(TagCategory.FILESIZE, "medium"),
      new TagDescriptor(TagCategory.FILESIZE, "large"),
      fileSizeMassive,

      new TagDescriptor(TagCategory.DIMENSIONS, "tiny"),
      new TagDescriptor(TagCategory.DIMENSIONS, "small"),
      new TagDescriptor(TagCategory.DIMENSIONS, "medium"),
      new TagDescriptor(TagCategory.DIMENSIONS, "large"),
      dimensionsMassive,

      new TagDescriptor(TagCategory.LENGTH, "very_short"),
      new TagDescriptor(TagCategory.LENGTH, "short"),
      new TagDescriptor(TagCategory.LENGTH, "medium"),
      new TagDescriptor(TagCategory.LENGTH, "long"),
      new TagDescriptor(TagCategory.LENGTH, "very_long"),

      new TagDescriptor(TagCategory.MISC, "tagme"),

      metaHasAudio,
      new TagDescriptor(TagCategory.META, "video"),
      new TagDescriptor(TagCategory.META, "has_child"),
      new TagDescriptor(TagCategory.META, "has_parent"),
      new TagDescriptor(TagCategory.META, "has_collection"),
      new TagDescriptor(TagCategory.META, "animated"),

      ratingSafe,
      ratingQuestionable,
      ratingExplicit
    );

    var associations = Map.of(
      dimensionsMassive, List.of(new TagDescriptor(TagCategory.USERLAND, "absurd_res")),
      metaHasAudio, List.of(new TagDescriptor(TagCategory.USERLAND, "sound"), new TagDescriptor(TagCategory.USERLAND, "audio"), new TagDescriptor(TagCategory.USERLAND, "has_sound"), new TagDescriptor(TagCategory.USERLAND, "audio_warning")),

      ratingSafe, List.of(new TagDescriptor(TagCategory.RATING, "s")),
      ratingQuestionable, List.of(new TagDescriptor(TagCategory.RATING, "q")),
      ratingExplicit, List.of(new TagDescriptor(TagCategory.RATING, "e"))
    );

    for (var toEnsure : tags) {
      var tag = tagManager.ensureTag(toEnsure);
      var association = associations.get(toEnsure);

      if (tag != null && association != null && !association.isEmpty()) {
        for (var toAssociate : association) {
          var assocTag = tagManager.ensureTag(toAssociate);
          if (assocTag != null) {
            tagManager.setParent(toAssociate, toEnsure);
          }
        }
      }
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

  public static Messaging messaging() {
    return messaging;
  }

  public static MediaProcessor mediaProcessor() {
    return mediaProcessor;
  }

  public static TagManager tagManager() {
    return tagManager;
  }

  public static Elastic elastic() {
    return elastic;
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
