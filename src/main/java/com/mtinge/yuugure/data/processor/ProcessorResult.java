package com.mtinge.yuugure.data.processor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
@Accessors(fluent = true, chain = true)
@RequiredArgsConstructor
public class ProcessorResult {
  private boolean success = false;
  private String message = "";
  private MediaMeta meta;
  private List<String> tags;
  private final ProcessableUpload dequeued;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProcessorResult that = (ProcessorResult) o;
    return success == that.success && message.equals(that.message) && meta.equals(that.meta) && dequeued.equals(that.dequeued);
  }

  @Override
  public int hashCode() {
    return Objects.hash(success, message, meta, dequeued);
  }

  public static ProcessorResult readFrom(BsonReader reader) {
    var builder = new Builder();

    reader.readStartDocument();
    while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
      var name = reader.readName();
      switch (name) {
        case "success" -> builder.success(reader.readBoolean());
        case "message" -> builder.message(reader.readString());
        case "meta" -> builder.meta(MediaMeta.readFrom(reader));
        case "dequeued" -> builder.dequeued(ProcessableUpload.readFrom(reader));
        case "tags" -> {
          var tags = new LinkedList<String>();

          reader.readStartArray();
          {
            while ((reader.readBsonType()) == BsonType.STRING) {
              tags.add(reader.readString());
            }
          }
          reader.readEndArray();

          builder.tags(tags);
        }
        default -> System.err.println("Unknown name in ProcessorResult document: " + name);
      }
    }
    reader.readEndDocument();

    return builder.build();
  }

  public void writeTo(BsonWriter writer) {
    ProcessorResult.writeTo(writer, this);
  }

  public static void writeTo(BsonWriter writer, ProcessorResult result) {
    if (result.meta == null) {
      throw new IllegalArgumentException("The provided ProcessorResult has a null meta.");
    }

    writer.writeStartDocument();


    writer.writeName("success");
    writer.writeBoolean(result.success);

    writer.writeName("message");
    writer.writeString(Optional.ofNullable(result.message).orElse(""));

    writer.writeName("dequeued");
    result.dequeued.writeTo(writer);

    writer.writeName("meta");
    result.meta.writeTo(writer);

    writer.writeName("tags");
    writer.writeStartArray();
    {
      for (var tag : result.tags) {
        writer.writeString(tag);
      }
    }
    writer.writeEndArray();


    writer.writeEndDocument();
  }

  @RequiredArgsConstructor
  @Setter
  @Accessors(fluent = true, chain = true)
  public static final class Builder {
    private boolean success;
    private String message;
    private MediaMeta meta;
    private List<String> tags;
    private ProcessableUpload dequeued;

    public ProcessorResult build() {
      return new ProcessorResult(dequeued).success(success).message(message).tags(tags).meta(meta);
    }
  }
}
