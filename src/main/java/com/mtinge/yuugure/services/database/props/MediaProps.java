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
public class MediaProps {
  /**
   * The SHA256 of the file.
   */
  private String sha256 = null;
  /**
   * The MD5 of the file.
   */
  private String md5 = null;
  /**
   * The PHASH of the file. Not yet implemented.
   */
  private String phash = null;
  /**
   * The mime of the file.
   */
  private String mime = null;
}
