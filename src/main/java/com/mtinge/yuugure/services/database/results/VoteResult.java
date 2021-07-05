package com.mtinge.yuugure.services.database.results;

import lombok.AllArgsConstructor;

/**
 * A result container that lets us build contextual responses for the websocket by reporting
 * previous state and current state.
 */
@AllArgsConstructor
public class VoteResult {
  public final boolean updated;

  public final boolean isActive;
  public final boolean wasActive;

  public final boolean isUpvote;
  public final boolean wasUpvote;
}
