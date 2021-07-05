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
public class BookmarkProps {
  /**
   * Whether or not this bookmark is currently active.
   */
  private Boolean active = null;
  /**
   * Whether or not this bookmark is public.
   */
  private Boolean isPublic = null;
  /**
   * The upload this bookmark points to.
   */
  private Integer upload = null;
  /**
   * The account that created this bookmark.
   */
  private Integer account = null;
}
