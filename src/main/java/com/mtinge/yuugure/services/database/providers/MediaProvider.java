package com.mtinge.yuugure.services.database.providers;

import com.mtinge.QueryBuilder.QueryBuilder;
import com.mtinge.QueryBuilder.ops.join.Join;
import com.mtinge.yuugure.data.postgres.DBMedia;
import com.mtinge.yuugure.services.database.Database;
import com.mtinge.yuugure.services.database.props.MediaProps;
import com.mtinge.yuugure.services.database.results.Result;
import org.jdbi.v3.core.Handle;

public class MediaProvider extends Provider<DBMedia, MediaProps> {
  @Override
  public Result<DBMedia> create(MediaProps props, Handle handle) {
    requireTransaction(handle);

    return Result.fromValue(
      Database.firstOrNull(
        QueryBuilder.insert("media")
          .columns("sha256", "md5", "phash", "mime")
          .values(":sha256", ":md5", ":phash", ":mime")
          .returning("*")
          .bind("sha256", props.sha256())
          .bind("md5", props.md5())
          .bind("phash", props.phash())
          .bind("mime", props.mime())
          .toQuery(handle),
        DBMedia.class
      )
    );
  }

  @Override
  public DBMedia read(int id, Handle handle) {
    return Database.firstOrNull(
      QueryBuilder.select("*")
        .from("media")
        .where("id", ":id")
        .bind("id", id)
        .toQuery(handle),
      DBMedia.class
    );
  }

  public DBMedia readBySha(String sha256, Handle handle) {
    return Database.firstOrNull(
      QueryBuilder.select("*")
        .from("media")
        .where("sha256", ":sha256")
        .bind("sha256", sha256)
        .toQuery(handle),
      DBMedia.class
    );
  }

  public DBMedia readByUpload(int uploadId, Handle handle) {
    // the join is faster than a nested query based on testing
    return Database.firstOrNull(
      QueryBuilder.select("m.*")
        .from("upload u")
        .join(Join.inner("media m", "m.id = u.media"))
        .where("u.id", ":id")
        .bind("id", uploadId)
        .toQuery(handle),
      DBMedia.class
    );
  }

  @Override
  public Result<DBMedia> update(int id, MediaProps updated, Handle handle) {
    var query = QueryBuilder.update("media").where("id", ":id").bind("id", id);

    if (updated.sha256() != null && !updated.sha256().isBlank()) {
      query
        .set("sha256", ":sha256")
        .bind("sha256", updated.sha256());
    }

    if (updated.md5() != null && !updated.md5().isBlank()) {
      query
        .set("md5", ":md5")
        .bind("md5", updated.md5());
    }

    if (updated.phash() != null && !updated.phash().isBlank()) {
      query
        .set("phash", ":phash")
        .bind("phash", updated.phash());
    }

    if (updated.mime() != null && !updated.mime().isBlank()) {
      query
        .set("mime", ":mime")
        .bind("mime", updated.mime());
    }

    return Result.fromValue(
      Database.firstOrNull(
        query.toQuery(handle),
        DBMedia.class
      )
    );
  }

  @Override
  public Result<DBMedia> delete(int id, Handle handle) {
    throw new Error("Operation Not Supported");
  }
}
