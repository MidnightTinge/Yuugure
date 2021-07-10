package com.mtinge.yuugure.scripts;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.ThreadFactories;
import com.mtinge.yuugure.core.comments.Renderer;
import com.mtinge.yuugure.data.postgres.DBComment;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A callable script to re-render all uploads. Uses a {@link java.util.concurrent.ThreadPoolExecutor}
 * to process jobs. Comments are locked via FOR UPDATE until rendering is complete and the entire
 * process is wrapped in a transactional block until the executor terminates.
 */
public class RenderAllComments extends RunnableScript {
  private static final Logger logger = LoggerFactory.getLogger(RenderAllComments.class);
  private static final AtomicBoolean canProcess = new AtomicBoolean(false);

  public RenderAllComments() {
    super("renderAllComments");
  }

  @Override
  public void run(LinkedList<String> args) {
    RenderAllComments.run();
  }

  public static void run() {
    App.database().jdbi().useHandle(handle -> {
      var handled = false;
      handle.begin();
      try {
        canProcess.set(true);
        var executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), ThreadFactories.prefixed("RenderAll/"));
        var jobs = handle.createQuery("SELECT * FROM comment WHERE true FOR UPDATE")
          .mapTo(DBComment.class)
          .collect(Collectors.toList());
        for (var job : jobs) {
          executor.submit(new Worker(job, handle, comment -> logger.info("[producer] Comment {} finished.", comment.id)));
        }
        executor.shutdown();
        executor.awaitTermination(30L, TimeUnit.DAYS);

        logger.info("All jobs done");
        handle.commit();
        handled = true;
      } catch (Exception e) {
        canProcess.set(false);
        logger.error("Failed to render comments", e);
      } finally {
        if (!handled) {
          handle.rollback();
        }
      }
    });
  }

  private static class Worker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger("RenderAllWorker");

    private final DBComment comment;
    private final Handle handle;
    private final Consumer<DBComment> onComplete;

    Worker(DBComment comment, Handle handle, Consumer<DBComment> onComplete) {
      this.comment = comment;
      this.handle = handle;
      this.onComplete = onComplete;
    }

    @Override
    public void run() {
      if (canProcess.get()) {
        logger.info("Rendering comment {}", comment.id);
        var mutated = handle.createQuery("UPDATE comment SET content_rendered = :content WHERE id = :id RETURNING *")
          .bind("content", Renderer.render(comment.contentRaw))
          .bind("id", comment.id)
          .mapTo(DBComment.class)
          .first();
        onComplete.accept(mutated);
      }
    }
  }
}
