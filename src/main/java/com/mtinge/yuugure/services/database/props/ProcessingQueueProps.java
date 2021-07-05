package com.mtinge.yuugure.services.database.props;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(fluent = true)
public class ProcessingQueueProps {
  /**
   * The upload ID we should be processing.
   */
  private Integer upload = null;
  /**
   * When this item was originally enqueued.
   */
  private Timestamp queuedAt = null;
  /**
   * Whether or not this item has been dequeued.
   */
  private Boolean dequeued = null;
  /**
   * Whether or not this ProcessorQueue errored.
   */
  private Boolean errored = null;
  /**
   * The error associated with the most recent processor failure. Can be out of date if a re-run has
   * a successful completion, always check {@link #errored}.
   */
  private String errorText = null;
  /**
   * Whether or not this ProcessorQueue has finished. Can be false when {@link #errored} is true.
   */
  private Boolean finished = null;
}
