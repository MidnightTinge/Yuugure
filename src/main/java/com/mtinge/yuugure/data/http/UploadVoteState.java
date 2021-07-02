package com.mtinge.yuugure.data.http;

import com.squareup.moshi.Json;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class UploadVoteState {
  @Json(name = "total_upvotes")
  public final int totalUpvotes;
  @Json(name = "total_downvotes")
  public final int totalDownvotes;
  @Json(name = "voted")
  public final boolean weVoted;
  @Json(name = "is_upvote")
  public final boolean weUpvoted;
}
