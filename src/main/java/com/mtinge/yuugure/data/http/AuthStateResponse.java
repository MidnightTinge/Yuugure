package com.mtinge.yuugure.data.http;

import com.squareup.moshi.Json;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class AuthStateResponse {
  public final boolean authenticated;
  public final SafeAccount account;
}
