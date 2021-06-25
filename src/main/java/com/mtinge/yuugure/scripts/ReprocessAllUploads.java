package com.mtinge.yuugure.scripts;

import com.mtinge.yuugure.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

public class ReprocessAllUploads extends RunnableScript {
  private static final Logger logger = LoggerFactory.getLogger(ReprocessAllUploads.class);

  public ReprocessAllUploads() {
    super("reprocessAllUploads");
  }

  @Override
  public void run(LinkedList<String> args) {
    ReprocessAllUploads.run();
  }

  public static void run() {
    var updated = App.database().jdbi().withHandle(handle ->
      handle.createUpdate("UPDATE processing_queue SET dequeued=false, finished=false, errored = false WHERE dequeued AND (finished OR errored)").execute()
    );

    if (updated != 0) {
      logger.info("Requeued {} media. Waking workers...", updated);
    } else {
      logger.info("No media to requeue, job done.");
    }

    App.mediaProcessor().wakeWorkers();
  }
}
