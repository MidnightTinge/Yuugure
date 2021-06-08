package com.mtinge.yuugure.data.http;

import com.squareup.moshi.Json;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class AuthStateResponse {
  public final boolean authenticated;
  @Json(name = "account_id")
  public final Integer accountId;
}
