package com.mtinge.yuugure.data.http;

import com.mtinge.yuugure.data.postgres.DBUpload;
import com.mtinge.yuugure.data.postgres.states.UploadState;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public final class TaggedUpload {
  public final DBUpload upload;
  public final UploadState state;
  public final List<Integer> tags;

  public TaggedUpload(DBUpload upload, List<Integer> tags) {
    this.upload = upload;
    this.state = UploadState.fromDb(upload);
    this.tags = tags;
  }
}
