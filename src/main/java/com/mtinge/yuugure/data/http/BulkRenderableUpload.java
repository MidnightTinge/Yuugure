package com.mtinge.yuugure.data.http;

import com.mtinge.yuugure.data.postgres.DBMedia;
import com.mtinge.yuugure.data.postgres.DBMediaMeta;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class BulkRenderableUpload {
  public final Map<Integer, SafeAccount> accounts;
  public final Map<Integer, SafeTag> tags;
  public final Map<Integer, DBMedia> medias;
  public final Map<Integer, DBMediaMeta> metas;
  public final List<ExtendedUpload> uploads;
}
