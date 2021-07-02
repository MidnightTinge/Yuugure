package com.mtinge.yuugure.data.http;

import com.squareup.moshi.Json;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class UploadBookmarkState {
  @Json(name = "total_public")
  public final int totalPublic;
  @Json(name = "bookmarked")
  public final boolean weBookmarked;
  @Json(name = "bookmarked_publicly")
  public final boolean weBookmarkedPublicly;
}
