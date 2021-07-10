package com.mtinge.yuugure.data.http;

import com.mtinge.yuugure.data.postgres.DBComment;
import com.squareup.moshi.Json;
import lombok.AllArgsConstructor;

import java.sql.Timestamp;

@AllArgsConstructor
public class SafeComment {
  public final int id;
  public final Integer parent;
  public final int account;
  public final Timestamp timestamp;

  @Json(name = "content_raw")
  public final String contentRaw;
  @Json(name = "content_rendered")
  public final String contentRendered;

  public static SafeComment fromDb(DBComment comment) {
    return new SafeComment(comment.id, comment.parent, comment.account, comment.timestamp, comment.contentRaw, comment.contentRendered);
  }
}
