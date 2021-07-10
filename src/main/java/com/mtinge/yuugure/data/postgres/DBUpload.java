package com.mtinge.yuugure.data.postgres;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.beans.ConstructorProperties;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

public class DBUpload {
  @ColumnName("id")
  public final int id;
  @ColumnName("media")
  public final int media;
  @ColumnName("parent")
  public final int parent;
  @ColumnName("owner")
  public final int owner;
  @ColumnName("upload_date")
  public final Timestamp uploadDate;
  @ColumnName("state")
  public final long state;

  @ConstructorProperties({"id", "media", "parent", "owner", "upload_date", "state"})
  public DBUpload(int id, int media, int parent, int owner, Timestamp uploadDate, long state) {
    this.id = id;
    this.media = media;
    this.parent = parent;
    this.owner = owner;
    this.uploadDate = uploadDate;
    this.state = state;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DBUpload dbUpload = (DBUpload) o;

    return id == dbUpload.id
      && media == dbUpload.media
      && parent == dbUpload.parent
      && owner == dbUpload.owner
      && state == dbUpload.state
      && uploadDate.toInstant().toEpochMilli() == dbUpload.uploadDate.toInstant().toEpochMilli();
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, media, parent, owner, uploadDate, state);
  }

  public static DBUpload readFrom(BsonReader reader) {
    var builder = new Builder();

    reader.readStartDocument();
    while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
      var name = reader.readName();
      switch (name) {
        case "id" -> builder.id(reader.readInt32());
        case "media" -> builder.media(reader.readInt32());
        case "parent" -> builder.parent(reader.readInt32());
        case "owner" -> builder.owner(reader.readInt32());
        case "uploadDate" -> builder.uploadDate(Timestamp.from(Instant.ofEpochMilli(reader.readInt64())));
        case "state" -> builder.state(reader.readInt64());
        default -> System.err.println("Unknown name in DBUpload document: " + name);
      }
    }
    reader.readEndDocument();

    return builder.build();
  }

  public void writeTo(BsonWriter writer) {
    DBUpload.writeTo(writer, this);
  }

  public static void writeTo(BsonWriter writer, DBUpload upload) {
    writer.writeStartDocument();


    writer.writeName("id");
    writer.writeInt32(upload.id);

    writer.writeName("media");
    writer.writeInt32(upload.media);

    writer.writeName("parent");
    writer.writeInt32(upload.parent);

    writer.writeName("owner");
    writer.writeInt32(upload.owner);

    writer.writeName("uploadDate");
    writer.writeInt64(upload.uploadDate.toInstant().toEpochMilli());

    writer.writeName("state");
    writer.writeInt64(upload.state);


    writer.writeEndDocument();
  }

  @Setter
  @Accessors(fluent = true)
  public static final class Builder {
    private int id;
    private int media;
    private int parent;
    private int owner;
    private Timestamp uploadDate;
    private long state;

    public DBUpload build() {
      return new DBUpload(id, media, parent, owner, uploadDate, state);
    }
  }
}
