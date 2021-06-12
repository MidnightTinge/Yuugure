package com.mtinge.yuugure.data.postgres;

import lombok.AllArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;

@AllArgsConstructor
public class DBMediaMeta {
  public final int id;
  public final int media;
  public final int width;
  public final int height;
  public final boolean video;
  public final long videoDuration;
  public final boolean hasAudio;
  public final long audioDuration;

  public static final RowMapper<DBMediaMeta> Mapper = (r, ctx) -> new DBMediaMeta(
    r.getInt("id"),
    r.getInt("media"),
    r.getInt("width"),
    r.getInt("height"),
    r.getBoolean("video"),
    r.getLong("video_duration"),
    r.getBoolean("has_audio"),
    r.getLong("audio_duration")
  );
}
