package com.mtinge.yuugure.data.http;

import com.mtinge.RateLimit.CheckResult;
import com.squareup.moshi.Json;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RateLimitResponse {
  public final long maximum;
  public final long items;
  public final long period;
  @Json(name = "minimum_wait")
  public final long minimumWait;

  public static RateLimitResponse fromCheck(CheckResult result) {
    return new RateLimitResponse(result.windowMaximum, result.itemsInWindow, result.window.toMillis(), result.nextAvailable.toMillis());
  }
}
