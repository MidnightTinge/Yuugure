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
public class CommentProps {
  /**
   * This comment's ancestor, typically present for replies.
   */
  private Integer parent = null;
  /**
   * The ID of the account who created this comment.
   */
  private Integer account = null;
  /**
   * Whether or not this comment is active.
   */
  private Boolean active = null;
  /**
   * When this comment was created.
   */
  private Timestamp timestamp = null;
  /**
   * The target type for this comment, e.g. "upload".
   */
  private String targetType = null;
  /**
   * The target ID for this comment. If targetType was "upload", then this would correspond to the
   * ID of the upload that is being commented on.
   */
  private Integer targetId = null;
  /**
   * The raw, un-processed content.
   */
  private String contentRaw = null;
  /**
   * The rendered content, e.g. passed through markdown rendering.
   */
  private String contentRendered = null;
}
