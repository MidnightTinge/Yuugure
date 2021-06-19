package com.mtinge.yuugure.data.http;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.LinkedList;

@AllArgsConstructor
@Getter
public final class AuthResponse extends InputAwareResponse {
  /**
   * Whether or not the registration was successful
   */
  @Setter
  public boolean authed;

  public AuthResponse() {
    super();
    this.authed = false;
    this.inputErrors = new HashMap<>();
    this.errors = new LinkedList<>();
  }
}
