package com.mtinge.yuugure.data.http;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SearchResult {
  public final SearchPagination page;
  public final BulkRenderableUpload result;
}
