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
public class UploadProps {
  /**
   * The media ID associated with this upload.
   */
  private Integer media = null;
  /**
   * The upload ID that is an ancestor of this upload, typically the "master" that this upload would
   * be a re-encode of.
   */
  private Integer parent = null;
  /**
   * The account ID that uploaded this file.
   */
  private Integer owner = null;
  /**
   * The date this file was uploaded.
   */
  private Timestamp uploadDate = null;
  /**
   * The {@link com.mtinge.yuugure.core.States.Upload State} of this upload.
   */
  private Long state = null;
}
