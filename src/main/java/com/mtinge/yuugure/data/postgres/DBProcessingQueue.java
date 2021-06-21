package com.mtinge.yuugure.data.postgres;

import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.jdbi.v3.core.mapper.RowMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@AllArgsConstructor
@ToString
public class DBProcessingQueue {
  public final int id;
  public final int upload;
  public final Timestamp queuedAt;
  public final boolean dequeued;
  public final boolean errored;
  public final String errorText;
  public final boolean finished;

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

  public static final RowMapper<DBProcessingQueue> Mapper = (r, ctx) -> new DBProcessingQueue(
    r.getInt("id"),
    r.getInt("upload"),
    r.getTimestamp("queued_at"),
    r.getBoolean("dequeued"),
    r.getBoolean("errored"),
    r.getString("error_text"),
    r.getBoolean("finished")
  );

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
