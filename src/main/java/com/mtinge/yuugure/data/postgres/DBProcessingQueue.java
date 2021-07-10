package com.mtinge.yuugure.data.postgres;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.beans.ConstructorProperties;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

public class DBProcessingQueue {
  @ColumnName("id")
  public final int id;
  @ColumnName("upload")
  public final int upload;
  @ColumnName("queued_at")
  public final Timestamp queuedAt;
  @ColumnName("dequeued")
  public final boolean dequeued;
  @ColumnName("errored")
  public final boolean errored;
  @ColumnName("error_text")
  public final String errorText;
  @ColumnName("finished")
  public final boolean finished;

  @ConstructorProperties({"id", "upload", "queued_at", "dequeued", "errored", "error_text", "finished"})
  public DBProcessingQueue(int id, int upload, Timestamp queuedAt, boolean dequeued, boolean errored, String errorText, boolean finished) {
    this.id = id;
    this.upload = upload;
    this.queuedAt = queuedAt;
    this.dequeued = dequeued;
    this.errored = errored;
    this.errorText = errorText;
    this.finished = finished;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DBProcessingQueue that = (DBProcessingQueue) o;

    return id == that.id
      && upload == that.upload
      && dequeued == that.dequeued
      && errored == that.errored
      && finished == that.finished
      && queuedAt.toInstant().toEpochMilli() == that.queuedAt.toInstant().toEpochMilli()
      && Objects.equals(errorText, that.errorText);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, upload, queuedAt, dequeued, errored, errorText, finished);
  }

  public static DBProcessingQueue readFrom(BsonReader reader) {
    var builder = new Builder();

    reader.readStartDocument();
    while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
      var name = reader.readName();
      switch (name) {
        case "id" -> builder.id(reader.readInt32());
        case "upload" -> builder.upload(reader.readInt32());
        case "queuedAt" -> builder.queuedAt(Timestamp.from(Instant.ofEpochMilli(reader.readInt64())));
        case "dequeued" -> builder.dequeued(reader.readBoolean());
        case "errored" -> builder.errored(reader.readBoolean());
        case "errorText" -> {
          try {
            builder.errorText(reader.readString());
          } catch (Exception e) {
            // ignored, assumed empty string.
          }
        }
        case "finished" -> builder.finished(reader.readBoolean());
        default -> System.err.println("Unknown name in DBProcessingQueue document: " + name);
      }
    }
    reader.readEndDocument();

    return builder.build();
  }

  public void writeTo(BsonWriter writer) {
    DBProcessingQueue.writeTo(writer, this);
  }

  public static void writeTo(BsonWriter writer, DBProcessingQueue dbProcessingQueue) {
    writer.writeStartDocument();


    writer.writeName("id");
    writer.writeInt32(dbProcessingQueue.id);

    writer.writeName("upload");
    writer.writeInt32(dbProcessingQueue.upload);

    writer.writeName("queuedAt");
    writer.writeInt64(dbProcessingQueue.queuedAt.toInstant().toEpochMilli());

    writer.writeName("dequeued");
    writer.writeBoolean(dbProcessingQueue.dequeued);

    writer.writeName("errored");
    writer.writeBoolean(dbProcessingQueue.errored);

    if (dbProcessingQueue.errorText != null) {
      writer.writeName("errorText");
      writer.writeString(dbProcessingQueue.errorText);
    }

    writer.writeName("finished");
    writer.writeBoolean(dbProcessingQueue.finished);


    writer.writeEndDocument();
  }

  @Setter
  @Accessors(fluent = true)
  public static final class Builder {
    private int id;
    private int upload;
    private Timestamp queuedAt;
    private boolean dequeued;
    private boolean errored;
    private String errorText;
    private boolean finished;

    public DBProcessingQueue build() {
      return new DBProcessingQueue(id, upload, queuedAt, dequeued, errored, errorText, finished);
    }
  }
}
