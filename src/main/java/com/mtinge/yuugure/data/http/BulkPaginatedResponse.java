package com.mtinge.yuugure.data.http;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BulkPaginatedResponse {
  public final BulkRenderableUpload uploads;
  public final int max;
}
