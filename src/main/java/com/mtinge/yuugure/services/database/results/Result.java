package com.mtinge.yuugure.services.database.results;

import com.mtinge.yuugure.services.database.providers.Provider;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class Result<T> {
  protected final List<String> errors;

  private T resource;
  private boolean success;
  private Integer failCode;

  public Result(@Nullable T resource, boolean success, @Nullable Integer failCode) {
    this.resource = resource;
    this.success = success;
    this.failCode = failCode;
    this.errors = new LinkedList<>();
  }

  public void addError(String error) {
    this.errors.add(error);
  }

  public static <T> Result<T> fail(@NotNull Integer failCode) {
    return new Result<T>(null, false, failCode);
  }

  public static <T> Result<T> fail(@NotNull Integer failCode, String errorMessage) {
    var result = new Result<T>(null, false, failCode);
    result.addError(errorMessage);

    return result;
  }

  public static <T> Result<T> success(@NotNull T resource) {
    return new Result<T>(resource, true, null);
  }

  public static <T> Result<T> fromValue(@Nullable T resource) {
    return new Result<>(resource, resource != null, resource == null ? Provider.FAIL_UNKNOWN : null);
  }
}
