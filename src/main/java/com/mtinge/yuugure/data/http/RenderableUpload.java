package com.mtinge.yuugure.data.http;

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
}
