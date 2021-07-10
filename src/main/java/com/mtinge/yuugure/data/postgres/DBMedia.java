package com.mtinge.yuugure.data.postgres;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.beans.ConstructorProperties;
import java.util.Objects;

public class DBMedia {
  @ColumnName("id")
  public final int id;
  @ColumnName("sha256")
  public final String sha256;
  @ColumnName("md5")
  public final String md5;
  @ColumnName("phash")
  public final String phash;
  @ColumnName("mime")
  public final String mime;

  @ConstructorProperties({"id", "sha256", "md5", "phash", "mime"})
  public DBMedia(int id, String sha256, String md5, String phash, String mime) {
    this.id = id;
    this.sha256 = sha256;
    this.md5 = md5;
    this.phash = phash;
    this.mime = mime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DBMedia dbMedia = (DBMedia) o;

    return id == dbMedia.id
      && Objects.equals(sha256, dbMedia.sha256)
      && Objects.equals(md5, dbMedia.md5)
      && Objects.equals(phash, dbMedia.phash)
      && Objects.equals(mime, dbMedia.mime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, sha256, md5, phash, mime);
  }

  public static DBMedia readFrom(BsonReader reader) {
    var builder = new Builder();

    reader.readStartDocument();
    while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
      var name = reader.readName();
      switch (name) {
        case "id" -> builder.id(reader.readInt32());
        case "sha256" -> builder.sha256(reader.readString());
        case "md5" -> builder.md5(reader.readString());
        case "phash" -> builder.phash(reader.readString());
        case "mime" -> builder.mime(reader.readString());
        default -> System.err.println("Unknown name in DBMedia document: " + name);
      }
    }
    reader.readEndDocument();

    return builder.build();
  }

  public void writeTo(BsonWriter writer) {
    DBMedia.writeTo(writer, this);
  }

  public static void writeTo(BsonWriter writer, DBMedia media) {
    writer.writeStartDocument();


    writer.writeName("id");
    writer.writeInt32(media.id);

    writer.writeName("sha256");
    writer.writeString(media.sha256);

    writer.writeName("md5");
    writer.writeString(media.md5);

    writer.writeName("phash");
    writer.writeString(media.phash);

    writer.writeName("mime");
    writer.writeString(media.mime);


    writer.writeEndDocument();
  }

  @Setter
  @Accessors(fluent = true)
  public static final class Builder {
    private int id;
    private String sha256;
    private String md5;
    private String phash;
    private String mime;

    public Builder() {
      //
    }

    public DBMedia build() {
      return new DBMedia(id, sha256, md5, phash, mime);
    }
  }
}
