package com.mtinge.yuugure.data.postgres;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.beans.ConstructorProperties;

public class DBMediaMeta {
  @ColumnName("id")
  public final int id;
  @ColumnName("media")
  public final int media;
  @ColumnName("width")
  public final int width;
  @ColumnName("height")
  public final int height;
  @ColumnName("video")
  public final boolean video;
  @ColumnName("video_duration")
  public final double videoDuration;
  @ColumnName("has_audio")
  public final boolean hasAudio;

  @ConstructorProperties({"id", "media", "width", "height", "video", "video_duration", "has_audio"})
  public DBMediaMeta(int id, int media, int width, int height, boolean video, double videoDuration, boolean hasAudio) {
    this.id = id;
    this.media = media;
    this.width = width;
    this.height = height;
    this.video = video;
    this.videoDuration = videoDuration;
    this.hasAudio = hasAudio;
  }
}
