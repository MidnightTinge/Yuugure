package com.mtinge.yuugure.services.database.props;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(fluent = true)
public class VoteProps {
  /**
   * Whether or not this vote is currently active.
   */
  private Boolean active = null;
  /**
   * Whether or not this vote is an upvote. When false, it's a downvote.
   */
  private Boolean isUp = null;
  /**
   * The upload this vote pertains to.
   */
  private Integer upload = null;
  /**
   * The account that created this vote.
   */
  private Integer account = null;
}
