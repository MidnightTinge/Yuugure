package com.mtinge.yuugure.services.database.results;

import lombok.AllArgsConstructor;

/**
 * A result container that lets us build contextual responses for the websocket by reporting
 * previous state and current state.
 */
@AllArgsConstructor
public class BookmarkResult {
  public final boolean updated;

  public final boolean isPublic;
  public final boolean wasPublic;

  public final boolean isActive;
  public final boolean wasActive;
}
