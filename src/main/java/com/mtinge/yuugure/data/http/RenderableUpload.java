package com.mtinge.yuugure.data.http;

import com.mtinge.yuugure.data.postgres.DBMedia;
import com.mtinge.yuugure.data.postgres.DBMediaMeta;
import com.mtinge.yuugure.data.postgres.DBTag;
import com.mtinge.yuugure.data.postgres.DBUpload;
import com.mtinge.yuugure.data.postgres.states.UploadState;
import com.squareup.moshi.Json;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public final class RenderableUpload {
  public final DBUpload upload;
  public final DBMedia media;
  @Json(name = "media_meta")
  public final DBMediaMeta mediaMeta;
  public final SafeAccount owner;
  public final UploadState state;
  public final List<SafeTag> tags;

  public RenderableUpload(DBUpload upload, DBMedia media, DBMediaMeta mediaMeta, SafeAccount owner, List<SafeTag> tags) {
    this.upload = upload;
    this.media = media;
    this.mediaMeta = mediaMeta;
    this.owner = owner;
    this.tags = tags;
    this.state = UploadState.fromDb(upload);
  }

}
