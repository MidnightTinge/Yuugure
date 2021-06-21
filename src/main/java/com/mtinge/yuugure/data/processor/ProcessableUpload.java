package com.mtinge.yuugure.data.processor;

import com.mtinge.yuugure.data.postgres.DBMedia;
import com.mtinge.yuugure.data.postgres.DBProcessingQueue;
import com.mtinge.yuugure.data.postgres.DBUpload;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;

import java.util.Objects;

@AllArgsConstructor
@ToString
public class ProcessableUpload {
  public final DBProcessingQueue queueItem;
  public final DBUpload upload;
  public final DBMedia media;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProcessableUpload that = (ProcessableUpload) o;

    return Objects.equals(queueItem, that.queueItem)
      && Objects.equals(upload, that.upload)
      && Objects.equals(media, that.media);
  }

  @Override
  public int hashCode() {
    return Objects.hash(queueItem, upload, media);
  }

  public static ProcessableUpload readFrom(BsonReader reader) {
    var builder = new Builder();

    reader.readStartDocument();
    while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
      var name = reader.readName();
      switch (name) {
        case "queueItem" -> builder.queueItem(DBProcessingQueue.readFrom(reader));
        case "upload" -> builder.upload(DBUpload.readFrom(reader));
        case "media" -> builder.media(DBMedia.readFrom(reader));
        default -> System.err.println("Unknown name in ProcessableUpload document: " + name);
      }
    }
    reader.readEndDocument();

    return builder.build();
  }

  public void writeTo(BsonWriter writer) {
    ProcessableUpload.writeTo(writer, this);
  }

  public static void writeTo(BsonWriter writer, ProcessableUpload upload) {
    writer.writeStartDocument();


    writer.writeName("queueItem");
    upload.queueItem.writeTo(writer);

    writer.writeName("upload");
    upload.upload.writeTo(writer);

    writer.writeName("media");
    upload.media.writeTo(writer);


    writer.writeEndDocument();
  }

  @Setter
  @Accessors(fluent = true)
  public static class Builder {
    private DBProcessingQueue queueItem;
    private DBUpload upload;
    private DBMedia media;

    public ProcessableUpload build() {
      return new ProcessableUpload(queueItem, upload, media);
    }
  }
}
