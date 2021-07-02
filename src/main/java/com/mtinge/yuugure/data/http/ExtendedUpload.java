package com.mtinge.yuugure.data.http;

import com.mtinge.yuugure.data.postgres.DBUpload;
import com.mtinge.yuugure.data.postgres.states.UploadState;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public final class ExtendedUpload {
  public final DBUpload upload;
  public final UploadState state;
  public final List<Integer> tags;
  public final UploadBookmarkState bookmarks;
  public final UploadVoteState votes;

  public ExtendedUpload(DBUpload upload, List<Integer> tags, UploadBookmarkState bookmarks, UploadVoteState votes) {
    this.upload = upload;
    this.state = UploadState.fromDb(upload);
    this.tags = tags;
    this.bookmarks = bookmarks;
    this.votes = votes;
  }
}
