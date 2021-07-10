package com.mtinge.yuugure.data.http;

import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class BulkRenderableComment {
  public final Map<Integer, SafeAccount> accounts;
  public final List<SafeComment> comments;
}
