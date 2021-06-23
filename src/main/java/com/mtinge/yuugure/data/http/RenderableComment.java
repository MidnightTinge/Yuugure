package com.mtinge.yuugure.data.http;

import com.squareup.moshi.Json;
import lombok.AllArgsConstructor;

import java.sql.Timestamp;

@AllArgsConstructor
public class RenderableComment {
  public final int id;
  public final Timestamp timestamp;
  public final SafeAccount account;
  @Json(name = "content_raw")
  public final String contentRaw;
  @Json(name = "content_rendered")
  public final String contentRendered;
}
