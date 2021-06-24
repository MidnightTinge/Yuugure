package com.mtinge.yuugure.data.http;

import lombok.Getter;
import lombok.Setter;

@Getter
public class UploadResult extends InputAwareResponse {
  @Setter
  private boolean success = false;
  @Setter
  private RenderableUpload upload = null;
}
