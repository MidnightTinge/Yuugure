package com.mtinge.yuugure.data.http;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Getter
public final class AuthResponse {
  /**
   * Whether or not the registration was successful
   */
  public boolean authed;
  /**
   * Maps errors to their inputs, e.g. "passwords did not match" showing up under the "repeat"
   * input
   */
  public Map<String, List<String>> inputErrors;
  /**
   * A general list of errors not tied to any one input ("An internal server error occurred"/etc)
   */
  public List<String> errors;

  public AuthResponse() {
    this.authed = false;
    this.inputErrors = new HashMap<>();
    this.errors = new LinkedList<>();
  }

  public void addInputError(String input, String error) {
    this.inputErrors.compute(input, (k, v) -> {
      if (v == null) {
        v = new LinkedList<>();
      }

      v.add(error);
      return v;
    });
  }

  public void addError(String error) {
    this.errors.add(error);
  }

  public boolean hasErrors() {
    return !inputErrors.isEmpty() || !errors.isEmpty();
  }
}
