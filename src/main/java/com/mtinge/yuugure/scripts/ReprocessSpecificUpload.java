package com.mtinge.yuugure.scripts;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.MoshiFactory;
import com.mtinge.yuugure.data.postgres.DBProcessingQueue;
import com.mtinge.yuugure.data.processor.ProcessableUpload;
import com.mtinge.yuugure.data.processor.ProcessorResult;
import com.mtinge.yuugure.services.processor.MediaProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.LinkedList;

public class ReprocessSpecificUpload extends RunnableScript {
  private static final Logger logger = LoggerFactory.getLogger(ReprocessSpecificUpload.class);

  public ReprocessSpecificUpload() {
    super("reprocessSpecificUpload");
  }

  @Override
  public void run(LinkedList<String> args) {
    if (!args.isEmpty()) {
      var id = args.removeFirst();
      if (id.matches("^[0-9]+$")) {
        ReprocessSpecificUpload.run(Integer.parseInt(id));
      } else {
        logger.warn("Invalid argument uploadId, expected number");
      }
    } else {
      logger.warn("Missing argument uploadId");
    }
  }

  public static void run(int uploadId) {
    // Grab our ProcessingQueue item ensuring we don't already have a processor working on it.
    // States:
    //    (!dequeued) -> not in process
    //    (dequeued && (finished || errored)) -> not in process (we have a previous run)
    //    (dequeued && !(finished || errored)) -> run in progress
    var toProcess = App.database().jdbi().withHandle(handle -> {
      var handled = false;
      handle.begin();

      try {
        var item = handle.createQuery("SELECT * FROM processing_queue WHERE upload = :id AND NOT (dequeued AND NOT (finished OR errored)) FOR UPDATE")
          .bind("id", uploadId)
          .mapTo(DBProcessingQueue.class)
          .findFirst().orElse(null);
        if (item != null) {
          // mark as dequeued
          handle.createUpdate("UPDATE processing_queue SET dequeued=true, finished=false, errored=false WHERE upload = :id")
            .bind("id", uploadId)
            .execute();

          // get upload/media for ProcessableUpload constructor
          var upload = App.database().uploads.read(item.upload, handle);
          var media = App.database().media.read(upload.media, handle);

          handle.commit();
          handled = true;

          return new ProcessableUpload(item, upload, media);
        }
      } catch (Exception e) {
        logger.error("Failed to dequeue upload {}.", uploadId, e);
      } finally {
        if (!handled) {
          handle.rollback();
        }
      }
      return null;
    });

    if (toProcess != null) {
      try {
        logger.info("Dequeued upload, starting processor...");
        var fullPath = Path.of(App.config().upload.finalDir, toProcess.media.sha256 + ".full");
        var thumbPath = Path.of(App.config().upload.finalDir, toProcess.media.sha256 + ".thumb");
        var result = MediaProcessor.Process(toProcess, fullPath, thumbPath);
        final ProcessorResult _res = result;
        var handleRes = App.database().jdbi().withHandle(handle -> {
          handle.begin();

          var res = App.database().processors.handleResult(_res, handle);
          if (res.isSuccess()) {
            handle.commit();
          } else {
            handle.rollback();
          }

          return res;
        });

        if (handleRes.isSuccess()) {
          logger.info("Job done. Result:" + MoshiFactory.create().adapter(Object.class).toJson(result));
        } else {
          logger.warn("Job failed. Errors: " + String.join("\n", handleRes.getErrors()));
        }
      } catch (Exception e) {
        logger.error("Failed to process upload {}.", uploadId, e);
      }
    } else {
      logger.info("No items were dequeued. Ensure the upload exists and is not already being processed.");
    }
  }
}
