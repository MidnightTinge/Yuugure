package com.mtinge.yuugure.data.postgres.states;

import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.data.postgres.DBUpload;
import com.squareup.moshi.Json;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UploadState {
  @Json(name = "PRIVATE")
  public final boolean isPrivate;
  @Json(name = "DELETED")
  public final boolean deleted;
  @Json(name = "DMCA")
  public final boolean dmca;
  @Json(name = "LOCKED_TAGS")
  public final boolean lockedTags;
  @Json(name = "LOCKED_COMMENTS")
  public final boolean lockedComments;
  @Json(name = "MODERATION_QUEUED")
  public final boolean moderationQueued;

  public UploadState(DBUpload upload) {
    this.isPrivate = States.flagged(upload.state, States.Upload.PRIVATE);
    this.deleted = States.flagged(upload.state, States.Upload.DELETED);
    this.dmca = States.flagged(upload.state, States.Upload.DMCA);
    this.lockedTags = States.flagged(upload.state, States.Upload.LOCKED_TAGS);
    this.lockedComments = States.flagged(upload.state, States.Upload.LOCKED_COMMENTS);
    this.moderationQueued = States.flagged(upload.state, States.Upload.MODERATION_QUEUED);
  }

  public static UploadState fromDb(DBUpload upload) {
    return new UploadState(upload);
  }
}
