package com.mtinge.yuugure.services.database.providers;

import com.mtinge.QueryBuilder.QueryBuilder;
import com.mtinge.QueryBuilder.ops.For.ForType;
import com.mtinge.QueryBuilder.ops.filter.Filter;
import com.mtinge.QueryBuilder.ops.join.Join;
import com.mtinge.QueryBuilder.ops.order.OrderType;
import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.TagManager.TagCategory;
import com.mtinge.yuugure.core.TagManager.TagDescriptor;
import com.mtinge.yuugure.data.postgres.DBProcessingQueue;
import com.mtinge.yuugure.data.postgres.DBTag;
import com.mtinge.yuugure.data.processor.ProcessableUpload;
import com.mtinge.yuugure.data.processor.ProcessorResult;
import com.mtinge.yuugure.services.database.Database;
import com.mtinge.yuugure.services.database.props.ProcessingQueueProps;
import com.mtinge.yuugure.services.database.results.Result;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProcessingQueueProvider extends Provider<DBProcessingQueue, ProcessingQueueProps> {
  private static final Logger logger = LoggerFactory.getLogger(ProcessingQueueProvider.class);

  @Override
  public Result<DBProcessingQueue> create(ProcessingQueueProps props, Handle handle) {
    requireTransaction(handle);

    return Result.fromValue(
      Database.firstOrNull(
        QueryBuilder.insert("processing_queue")
          .columns("upload")
          .values(":upload")
          .returning("*")
          .bind("upload", props.upload())
          .toQuery(handle),
        DBProcessingQueue.Mapper
      )
    );
  }

  @Override
  public DBProcessingQueue read(int id, Handle handle) {
    return Database.firstOrNull(
      QueryBuilder.select("*")
        .from("processing_queue")
        .where("id", ":id")
        .bind("id", id)
        .toQuery(handle),
      DBProcessingQueue.Mapper
    );
  }

  public DBProcessingQueue readForUpload(int uploadId, Handle handle) {
    return Database.firstOrNull(
      QueryBuilder.select("*")
        .from("processing_queue")
        .where("upload", ":upload")
        .bind("upload", uploadId)
        .toQuery(handle),
      DBProcessingQueue.Mapper
    );
  }

  public List<DBProcessingQueue> readActive(Handle handle) {
    return Database.toList(
      QueryBuilder.select("*")
        .from("processing_queue")
        .where(
          Filter.and(
            Filter.of("dequeued"),
            Filter.not(
              Filter.or(
                Filter.of("finished"),
                Filter.of("errored")
              )
            )
          )
        )
        .toQuery(handle),
      DBProcessingQueue.Mapper
    );
  }

  @Override
  public Result<DBProcessingQueue> update(int id, ProcessingQueueProps updated, Handle handle) {
    var builder = QueryBuilder.update("processing_queue").where("id", ":id").bind("id", id);

    if (updated.upload() != null) {
      builder.set("upload", ":upload")
        .bind("upload", updated.upload());
    }

    if (updated.queuedAt() != null) {
      builder.set("queuedAt", ":queuedAt")
        .bind("queuedAt", updated.queuedAt());
    }

    if (updated.dequeued() != null) {
      builder.set("dequeued", ":dequeued")
        .bind("dequeued", updated.dequeued());
    }

    if (updated.errored() != null) {
      builder.set("errored", ":errored")
        .bind("errored", updated.errored());
    }

    if (updated.errorText() != null && !updated.errorText().isBlank()) {
      builder.set("errorText", ":errorText")
        .bind("errorText", updated.errorText());
    }

    if (updated.finished() != null) {
      builder.set("finished", ":finished")
        .bind("finished", updated.finished());
    }

    return Result.fromValue(
      Database.firstOrNull(
        builder.toQuery(handle),
        DBProcessingQueue.Mapper
      )
    );
  }

  @Override
  public Result<DBProcessingQueue> delete(int id, Handle handle) {
    throw new Error("Operation Not Supported");
  }

  public Result<ProcessableUpload> dequeue(Handle handle) {
    if (!handle.isInTransaction()) {
      handle.begin();
    }

    try {
      // select and lock a row
      var id = Database.firstOrNull(
        QueryBuilder.select("id")
          .from("processing_queue")
          .where(Filter.not("dequeued"))
          .order("queued_at", OrderType.ASC)
          .limit(1)
          .withFor(ForType.UPDATE)
          .toQuery(handle),
        Database.intMapper("id")
      );

      if (id != null && id > 0) {
        var item = Database.first(
          QueryBuilder.update("processing_queue")
            .set("dequeued", "true")
            .where("id", ":id")
            .returning("*")
            .bind("id", id)
            .toQuery(handle),
          DBProcessingQueue.Mapper
        );
        var upload = App.database().uploads.read(item.upload, handle);
        var media = App.database().media.read(upload.media, handle);
        handle.commit();

        return Result.success(new ProcessableUpload(item, upload, media));
      } else {
        handle.commit();
        return new Result<>(null, true, null);
      }
    } catch (Exception e) {
      logger.error("Failed to dequeue an upload for processing.", e);
      handle.rollback();
      return Result.fail(FAIL_SQL);
    }
  }

  public Result<?> handleResult(ProcessorResult result, Handle handle) {
    requireTransaction(handle);

    // Update the media_meta
    App.database().mediaMeta.upsert(result.meta(), handle);

    // Lock the processor_queue row for updates
    handle.execute("SELECT 1 FROM processing_queue WHERE id = ? FOR UPDATE", result.dequeued().queueItem.id);

    // Update processing_queue state
    QueryBuilder.update("processing_queue")
      .set("finished", "true")
      .set("errored", ":errored")
      .set("error_text", ":error_text")
      .where("id", ":id")
      .bind("id", result.dequeued().queueItem.id)
      .bind("errored", !result.success())
      .bind("error_text", result.message())
      .toUpdate(handle)
      .execute();

    // Set system tags based on ProcessorResult state
    var tds = App.tagManager().ensureAll(result.tags().stream().map(TagDescriptor::parse).collect(Collectors.toList()), false);
    if (!tds.tags.isEmpty()) {
      // We ignore if this was true/false because it'll return false if the tags are the same which
      // can happen on a reprocess.
      App.database().tags.addTagsToUpload(result.dequeued().upload.id, tds.tags, handle);

      // Get current tags and filter out system tags (we're overriding with processor result)
      var curTags = Database.toList(
        QueryBuilder.select("t.*")
          .from("upload_tags ut")
          .join(Join.inner("tag t", "ut.tag = t.id"))
          .where("upload", ":upload")
          .bind("upload", result.dequeued().upload.id)
          .toQuery(handle),
        DBTag.Mapper
      ).stream()
        .filter(t -> t.category.equalsIgnoreCase(TagCategory.USERLAND.getName()))
        .map(t -> t.id)
        .collect(Collectors.toList());

      // Concat the processor result's tags
      var toSet = Stream
        .concat(tds.tags.stream().map(t -> t.id), curTags.stream())
        .collect(Collectors.toList());

      // Set tags
      App.elastic().setTagsForUpload(result.dequeued().upload.id, toSet);
      return new Result<>(null, true, null);
    } else {
      var res = new Result<>(null, false, FAIL_UNKNOWN);
      tds.messages.forEach(res::addError);

      return res;
    }
  }

}
