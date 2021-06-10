package com.mtinge.yuugure.core.adapters;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;

public class SqlTimestampAdapter extends JsonAdapter<Timestamp> {
  @Override
  public void toJson(JsonWriter writer, Timestamp value) throws IOException {
    if (value == null) {
      writer.nullValue();
    } else {
      writer.value(value.toInstant().toEpochMilli());
    }
  }

  @Override
  public Timestamp fromJson(JsonReader reader) throws IOException {
    if (reader.hasNext()) {
      if (reader.peek() == JsonReader.Token.NUMBER) {
        return Timestamp.from(Instant.ofEpochMilli(reader.nextLong()));
      }
    }

    return null;
  }
}
