package com.mtinge.yuugure.data.http;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ProfileResponse {
  public final boolean self;
  public final SafeAccount account;
}
