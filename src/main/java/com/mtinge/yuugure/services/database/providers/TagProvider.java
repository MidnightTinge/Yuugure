package com.mtinge.yuugure.services.database.providers;

import com.mtinge.QueryBuilder.QueryBuilder;
import com.mtinge.QueryBuilder.ops.filter.Filter;
import com.mtinge.QueryBuilder.ops.join.Join;
import com.mtinge.yuugure.data.postgres.DBTag;
import com.mtinge.yuugure.services.database.Database;
import com.mtinge.yuugure.services.database.props.TagProps;
import com.mtinge.yuugure.services.database.results.Result;
import org.jdbi.v3.core.Handle;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class TagProvider extends Provider<DBTag, TagProps> {
  @Override
  public Result<DBTag> create(TagProps props, Handle handle) {
    return Result.fromValue(
      Database.firstOrNull(
        QueryBuilder.insert("tag")
          .columns("parent", "category", "name", "assoc_type", "assoc_id")
          .values(":parent", ":category", ":name", ":assocType", ":assocId")
          .returning("*")
          .bind("parent", props.parent())
          .bind("category", requireNonNull(props.category()).getName())
          .bind("name", requireNonNull(props.name()))
          .bind("assocType", props.assocType())
          .bind("assocId", props.assocId())
          .toQuery(handle),
        DBTag.class
      )
    );
  }

  @Override
  public DBTag read(int id, Handle handle) {
    return Database.firstOrNull(
      QueryBuilder.select("*")
        .from("tag")
        .where("id", ":id")
        .bind("id", id)
        .toQuery(handle),
      DBTag.class
    );
  }

  public List<DBTag> read(List<Integer> ids, Handle handle) {
    return Database.toList(
      QueryBuilder.select("*")
        .from("tag")
        .where(Filter.in("id", ids.stream().map(String::valueOf).toArray(String[]::new)))
        .toQuery(handle),
      DBTag.class
    );
  }

  public List<DBTag> readForUpload(int id, Handle handle) {
    return Database.toList(
      QueryBuilder.select("t.*")
        .from("upload_tags ut")
        .join(Join.inner("tag t", "t.id = ut.tag"))
        .where("ut.upload", ":id")
        .bind("id", id)
        .toQuery(handle),
      DBTag.class
    );
  }


  @Override
  public Result<DBTag> update(int id, TagProps updated, Handle handle) {
    var query = QueryBuilder.update("tag")
      .where("id", ":id")
      .returning("*")
      .bind("id", id);

    if (updated.parent() != null) {
      query.set("parent", ":parent").bind("parent", updated.parent());
    }
    if (updated.category() != null) {
      query.set("category", ":category").bind("category", updated.category());
    }
    if (updated.name() != null) {
      query.set("name", ":name").bind("name", updated.name());
    }
    if (updated.assocType() != null || updated.assocId() != null) {
      query.set("assocType", ":assocType").bind("assocType", requireNonNull(updated.assocType()));
      query.set("assocId", ":assocId").bind("assocId", requireNonNull(updated.assocId()));
    }

    return Result.fromValue(
      Database.firstOrNull(
        query.toQuery(handle),
        DBTag.class
      )
    );
  }

  @Override
  public Result<DBTag> delete(int id, Handle handle) {
    throw new Error("Operation Not Supported");
  }

  public boolean addTagsToUpload(int id, List<DBTag> tags, Handle handle) {
    var batch = handle.prepareBatch("INSERT INTO upload_tags (upload, tag) VALUES (:upload, :tag) ON CONFLICT DO NOTHING");
    for (var tag : tags) {
      batch.bind("upload", id).bind("tag", tag.id).add();
    }

    var counts = batch.execute();
    for (var count : counts) {
      if (count > 0) {
        return true;
      }
    }

    return false;
  }

  public boolean removeTagsFromUpload(int id, List<DBTag> tags, Handle handle) {
    var batch = handle.prepareBatch("DELETE FROM upload_tags WHERE upload = :upload AND tag = :tag");
    for (var tag : tags) {
      batch.bind("upload", id).bind("tag", tag.id);
    }

    var counts = batch.execute();
    for (var count : counts) {
      if (count > 0) {
        return true;
      }
    }

    return false;
  }
}
