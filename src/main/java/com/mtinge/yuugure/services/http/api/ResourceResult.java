package com.mtinge.yuugure.services.http.api;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class ResourceResult<T> {
  public final FetchState state;
  public final T resource;

  public static <T> ResourceResult<T> notFound() {
    return new ResourceResult<>(FetchState.NOT_FOUND, null);
  }

  public static <T> ResourceResult<T> unauthorized() {
    return new ResourceResult<>(FetchState.UNAUTHORIZED, null);
  }

  public static <T> ResourceResult<T> OK(T resource) {
    if (resource == null) throw new IllegalArgumentException("An \"OK\" resource cannot be null.");
    return new ResourceResult<>(FetchState.OK, resource);
  }
}
