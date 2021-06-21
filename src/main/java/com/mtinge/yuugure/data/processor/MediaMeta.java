package com.mtinge.yuugure.data.processor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;

import java.util.Objects;

@Getter
@Setter
@Accessors(fluent = true, chain = true)
@ToString
public class MediaMeta {
  @Setter(AccessLevel.NONE)
  private int media;
  private int width = 0;
  private int height = 0;
  private boolean video = false;
  private double videoDuration = 0;
  private boolean hasAudio = false;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MediaMeta mediaMeta = (MediaMeta) o;

    return media == mediaMeta.media
      && width == mediaMeta.width
      && height == mediaMeta.height
      && video == mediaMeta.video
      && Double.compare(mediaMeta.videoDuration, videoDuration) == 0
      && hasAudio == mediaMeta.hasAudio;
  }

  @Override
  public int hashCode() {
    return Objects.hash(media, width, height, video, videoDuration, hasAudio);
  }

  public MediaMeta(int media) {
    this.media = media;
  }

  public static MediaMeta readFrom(BsonReader reader) {
    var builder = new Builder();

    reader.readStartDocument();
    while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
      var name = reader.readName();
      switch (name) {
        case "media" -> builder.media(reader.readInt32());
        case "width" -> builder.width(reader.readInt32());
        case "height" -> builder.height(reader.readInt32());
        case "video" -> builder.video(reader.readBoolean());
        case "videoDuration" -> builder.videoDuration(reader.readDouble());
        case "hasAudio" -> builder.hasAudio(reader.readBoolean());
        default -> System.err.println("Unknown name in MediaMeta document: " + name);
      }
    }
    reader.readEndDocument();

    return builder.build();
  }

  public void writeTo(BsonWriter writer) {
    MediaMeta.writeTo(writer, this);
  }

  public static void writeTo(BsonWriter writer, MediaMeta meta) {
    writer.writeStartDocument();


    writer.writeName("media");
    writer.writeInt32(meta.media);

    writer.writeName("width");
    writer.writeInt32(meta.width);

    writer.writeName("height");
    writer.writeInt32(meta.height);

    writer.writeName("video");
    writer.writeBoolean(meta.video);

    writer.writeName("videoDuration");
    writer.writeDouble(meta.videoDuration);

    writer.writeName("hasAudio");
    writer.writeBoolean(meta.hasAudio);


    writer.writeEndDocument();
  }

  @Setter
  @Accessors(fluent = true, chain = true)
  public static final class Builder {
    private int media;
    private int width = 0;
    private int height = 0;
    private boolean video = false;
    private double videoDuration = 0;
    private boolean hasAudio = false;

    public MediaMeta build() {
      return new MediaMeta(media)
        .width(width)
        .height(height)
        .video(video)
        .videoDuration(videoDuration)
        .hasAudio(hasAudio);
    }
  }
}
