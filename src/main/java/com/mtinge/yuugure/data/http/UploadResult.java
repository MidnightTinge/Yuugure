package com.mtinge.yuugure.data.http;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class UploadResult extends InputAwareResponse {
  private boolean success = false;
  private RenderableUpload upload = null;
  private List<String> notices;

  public UploadResult() {
    super();
    this.notices = new LinkedList<>();
  }

  public UploadResult addNotice(String notice) {
    this.notices.add(notice);
    return this;
  }
}
