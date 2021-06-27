package com.mtinge.yuugure.scripts;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.TagManager.TagDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Scanner;

public class AddTagsFromFilesystem extends RunnableScript {
  private static final Logger logger = LoggerFactory.getLogger(AddTagsFromFilesystem.class);

  public AddTagsFromFilesystem() {
    super("addTagsFromFilesystem");
  }

  @Override
  public void run(LinkedList<String> args) {
    if (!args.isEmpty()) {
      var file = Path.of(args.removeFirst()).toFile();
      if (!file.exists()) {
        logger.warn("The provided file ({}) does not exist.", file.toString());
      } else {
        AddTagsFromFilesystem.run(file);
      }
    } else {
      logger.warn("Missing file path.");
    }
  }

  public static void run(File file) {
    var lines = new LinkedList<String>();

    try (var scanner = new Scanner(new FileInputStream(file))) {
      while (scanner.hasNext()) {
        var line = scanner.nextLine();
        if (!line.isBlank() && !line.startsWith("#")) {
          lines.add(line);
        }
      }
      logger.info("Read {} lines", lines.size());
    } catch (Exception e) {
      logger.error("Failed to read file", e);
    }

    if (!lines.isEmpty()) {
      try {
        for (var line : lines) {
          try {
            var td = TagDescriptor.parse(line);
            if (td != null) {
              var tag = App.tagManager().ensureTag(td);
              if (tag == null) {
                logger.warn("Failed to add line {}", line);
              }
            } else {
              logger.warn("Discarding line '{}', invalid TagDescriptor.", line);
            }
          } catch (Exception e) {
            logger.error("Failed to add line {}", line, e);
          }
        }
        logger.info("All lines processed.");
      } catch (Exception e) {
        logger.error("Failed to add tags", e);
      }
    } else {
      logger.warn("No lines to process.");
    }
  }
}
