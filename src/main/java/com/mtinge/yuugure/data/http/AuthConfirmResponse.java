package com.mtinge.yuugure.data.http;

import com.squareup.moshi.Json;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthConfirmResponse extends InputAwareResponse {
  private boolean authenticated;
  @Json(name = "confirmation_token")
  private String confirmationToken;
}
