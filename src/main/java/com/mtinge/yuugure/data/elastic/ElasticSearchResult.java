package com.mtinge.yuugure.data.elastic;

import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class ElasticSearchResult {
  public final int pageCurrent;
  public final int pageMax;
  public final List<Integer> hits;
}
