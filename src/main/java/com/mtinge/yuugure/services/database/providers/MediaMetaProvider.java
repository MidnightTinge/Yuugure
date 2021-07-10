package com.mtinge.yuugure.services.database.providers;

import com.mtinge.QueryBuilder.QueryBuilder;
import com.mtinge.QueryBuilder.ops.For.ForType;
import com.mtinge.yuugure.data.postgres.DBMediaMeta;
import com.mtinge.yuugure.data.processor.MediaMeta;
import com.mtinge.yuugure.services.database.Database;
import com.mtinge.yuugure.services.database.results.Result;
import org.jdbi.v3.core.Handle;

public class MediaMetaProvider extends Provider<DBMediaMeta, MediaMeta> {
  @Override
  public Result<DBMediaMeta> create(MediaMeta props, Handle handle) {
    requireTransaction(handle);

    return Result.fromValue(
      Database.firstOrNull(
        QueryBuilder.insert("media_meta")
          .columns("media", "width", "height", "video", "video_duration", "has_audio")
          .values(":media", ":width", ":height", ":video", ":video_duration", ":has_audio")
          .bind("media", props.media())
          .bind("width", props.width())
          .bind("height", props.height())
          .bind("video", props.video())
          .bind("video_duration", props.videoDuration())
          .bind("has_audio", props.hasAudio())
          .toQuery(handle),
        DBMediaMeta.class
      )
    );
  }

  public Result<DBMediaMeta> upsert(MediaMeta props, Handle handle) {
    requireTransaction(handle);

    // lock the row
    var id = Database.firstOrNull(
      QueryBuilder.select("id")
        .from("media_meta")
        .where("media", ":media")
        .withFor(ForType.UPDATE)
        .bind("media", props.media())
        .toQuery(handle),
      Database.intMapper("id")
    );

    QueryBuilder builder;
    if (id == null || id == 0) {
      // the meta doesn't exist so we'll insert
      builder = QueryBuilder.insert("media_meta")
        .columns("media", "width", "height", "video", "video_duration", "has_audio", "filesize")
        .values(":media", ":width", ":height", ":video", ":video_duration", ":has_audio", ":filesize")
        .returning("*")
        .bind("media", props.media());
    } else {
      // the meta exists so we'll override
      builder = QueryBuilder.update("media_meta")
        .set("width", ":width")
        .set("height", ":height")
        .set("video", ":video")
        .set("video_duration", ":video_duration")
        .set("has_audio", ":has_audio")
        .set("filesize", ":filesize")
        .returning("*")
        .where("id", ":id")
        .bind("id", id);
    }

    return Result.fromValue(
      Database.firstOrNull(
        builder
          .bind("width", props.width())
          .bind("height", props.height())
          .bind("video", props.video())
          .bind("video_duration", props.videoDuration())
          .bind("has_audio", props.hasAudio())
          .bind("filesize", props.fileSize())
          .toQuery(handle),
        DBMediaMeta.class
      )
    );
  }

  @Override
  public DBMediaMeta read(int id, Handle handle) {
    return Database.firstOrNull(
      QueryBuilder.select("*")
        .from("media_meta")
        .where("id", ":id")
        .bind("id", id)
        .toQuery(handle),
      DBMediaMeta.class
    );
  }

  public DBMediaMeta readForMedia(int media, Handle handle) {
    return Database.firstOrNull(
      QueryBuilder.select("*")
        .from("media_meta")
        .where("media", ":media")
        .bind("media", media)
        .toQuery(handle),
      DBMediaMeta.class
    );
  }

  @Override
  public Result<DBMediaMeta> update(int id, MediaMeta updated, Handle handle) {
    return null;
  }

  @Override
  public Result<DBMediaMeta> delete(int id, Handle handle) {
    return null;
  }
}
