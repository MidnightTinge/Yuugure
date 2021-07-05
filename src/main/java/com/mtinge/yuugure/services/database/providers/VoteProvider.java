package com.mtinge.yuugure.services.database.providers;

import com.mtinge.QueryBuilder.QueryBuilder;
import com.mtinge.QueryBuilder.ops.filter.Filter;
import com.mtinge.yuugure.data.http.UploadVoteState;
import com.mtinge.yuugure.data.postgres.DBAccount;
import com.mtinge.yuugure.data.postgres.DBUpload;
import com.mtinge.yuugure.data.postgres.DBUploadVote;
import com.mtinge.yuugure.services.database.Database;
import com.mtinge.yuugure.services.database.props.VoteProps;
import com.mtinge.yuugure.services.database.results.Result;
import com.mtinge.yuugure.services.database.results.VoteResult;
import org.jdbi.v3.core.Handle;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class VoteProvider extends Provider<DBUploadVote, VoteProps> {
  @Override
  public Result<DBUploadVote> create(VoteProps props, Handle handle) {
    return null;
  }

  @Override
  public DBUploadVote read(int id, Handle handle) {
    throw new Error("Operation Not Supported: This table has a double-column key. Use a different read method.");
  }

  public DBUploadVote read(int account, int upload, Handle handle) {
    return Database.firstOrNull(
      QueryBuilder.select("*")
        .from("upload_vote")
        .where(Filter.and(
          Filter.of("account", ":account"),
          Filter.of("upload", ":upload")
        ))
        .bind("account", account)
        .bind("upload", upload)
        .toQuery(handle),
      DBUploadVote.Mapper
    );
  }

  public List<DBUploadVote> readForUpload(int uploadId, boolean includeInactive, Handle handle) {
    var builder = QueryBuilder.select("*")
      .from("upload_vote")
      .bind("upload", uploadId);

    var filter = Filter.of("upload", ":upload");

    if (!includeInactive) {
      filter = Filter.and(
        filter,
        Filter.of("active")
      );
    }

    return builder
      .where(filter)
      .toQuery(handle)
      .map(DBUploadVote.Mapper)
      .collect(Collectors.toList());
  }

  @Override
  public Result<DBUploadVote> update(int id, VoteProps updated, Handle handle) {
    throw new Error("Operation Not Supported: This table has a double-column key. Use a different update method.");
  }

  public Result<DBUploadVote> update(int account, int upload, VoteProps updated, Handle handle) {
    var query = QueryBuilder.update("upload_vote")
      .where(Filter.and(
        Filter.of("account", ":account"),
        Filter.of("upload", ":upload")
      ))
      .returning("*")
      .bind("account", account)
      .bind("upload", upload);

    if (updated.active() != null) {
      query.set("active", ":active").bind("active", updated.active());
    }

    if (updated.isUp() != null) {
      query.set("is_up", ":is_up").bind("is_up", updated.isUp());
    }

    if (updated.upload() != null) {
      query.set("upload", ":upload").bind("upload", updated.upload());
    }

    if (updated.account() != null) {
      query.set("account", ":account").bind("account", updated.account());
    }

    return Result.fromValue(
      Database.firstOrNull(
        query.toQuery(handle),
        DBUploadVote.Mapper
      )
    );
  }

  @Override
  public Result<DBUploadVote> delete(int id, Handle handle) {
    throw new Error("Operation Not Supported: This table has a double-column key. Use a different delete method.");
  }

  public Result<DBUploadVote> delete(int account, int upload, Handle handle) {
    return Result.fromValue(
      Database.firstOrNull(
        QueryBuilder.update("upload_vote")
          .set("active", ":active")
          .where(Filter.and(
            Filter.of("account", ":account"),
            Filter.of("upload", ":upload")
          ))
          .bind("active", false)
          .bind("account", account)
          .bind("upload", upload)
          .returning("*")
          .toQuery(handle),
        DBUploadVote.Mapper
      )
    );
  }

  public VoteResult handleFlip(DBAccount account, DBUpload upload, VoteProps props, Handle handle) {
    requireNonNull(props.isUp());
    requireNonNull(props.active());

    // lock the existing row for update if it exists
    var existing = handle.createQuery("SELECT * FROM upload_vote WHERE account = :account AND upload = :upload FOR UPDATE")
      .bind("account", account.id)
      .bind("upload", upload.id)
      .map(DBUploadVote.Mapper)
      .findFirst().orElse(null);

    // Create or update an existing vote
    var affected = handle.createUpdate("INSERT INTO upload_vote (account, upload, active, is_up) VALUES (:account, :upload, :active, :is_up) ON CONFLICT ON CONSTRAINT upload_vote_pkey DO UPDATE SET account = :account, upload = :upload, active = :active, is_up = :is_up")
      .bind("account", account.id)
      .bind("upload", upload.id)
      .bind("is_up", props.isUp())
      .bind("active", props.active())
      .execute();

    boolean isActive = props.active();
    boolean wasActive = existing != null && existing.active;

    boolean isUpvote = props.isUp();
    boolean wasUpvote = existing != null && existing.isUp;

    return new VoteResult(affected > 0, isActive, wasActive, isUpvote, wasUpvote);
  }

  public UploadVoteState getVoteStateForUpload(int uploadId, @Nullable DBAccount accountContext, Handle handle) {
    var dbVotes = readForUpload(uploadId, false, handle);

    DBUploadVote ourVote = null;
    int totalUpvotes = 0;
    int totalDownvotes = 0;

    for (var vote : dbVotes) {
      if (vote.isUp) {
        totalUpvotes++;
      } else {
        totalDownvotes++;
      }

      if (accountContext != null && ourVote == null) {
        if (vote.account == accountContext.id) {
          ourVote = vote;
        }
      }
    }

    return new UploadVoteState(totalUpvotes, totalDownvotes, ourVote != null, ourVote != null && ourVote.isUp);
  }
}
