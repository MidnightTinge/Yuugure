package com.mtinge.yuugure.data.http;

import com.mtinge.yuugure.data.postgres.states.UploadState;
import com.mtinge.yuugure.data.postgres.DBMedia;
import com.mtinge.yuugure.data.postgres.DBMediaMeta;
import com.mtinge.yuugure.data.postgres.DBUpload;
import com.squareup.moshi.Json;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class RenderableUpload {
  public final DBUpload upload;
  public final DBMedia media;
  @Json(name = "media_meta")
  public final DBMediaMeta mediaMeta;
  public final SafeAccount owner;
  public final UploadState state;

  public RenderableUpload(DBUpload upload, DBMedia media, DBMediaMeta mediaMeta, SafeAccount owner) {
    this.upload = upload;
    this.media = media;
    this.mediaMeta = mediaMeta;
    this.owner = owner;
    this.state = UploadState.fromDb(upload);
  }

}
