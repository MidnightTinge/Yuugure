package com.mtinge.yuugure.data.http;

import com.mtinge.yuugure.data.postgres.DBMedia;
import com.mtinge.yuugure.data.postgres.DBUpload;
import lombok.Getter;
import lombok.Setter;

@Getter
public class UploadResult extends InputAwareResponse {
  @Setter
  private boolean success = false;
  @Setter
  private DBMedia media = null;
  @Setter
  private DBUpload upload = null;
}
