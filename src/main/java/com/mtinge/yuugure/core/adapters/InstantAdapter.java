package com.mtinge.yuugure.core.adapters;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;
import java.time.Instant;

public class InstantAdapter extends JsonAdapter<Instant> {
  @Override
  public void toJson(JsonWriter writer, Instant value) throws IOException {
    if (value != null) {
      writer.value(value.toEpochMilli());
    } else {
      writer.nullValue();
    }
  }

  @Override
  public Instant fromJson(JsonReader reader) throws IOException {
    if (reader.peek() == JsonReader.Token.NUMBER) {
      return Instant.ofEpochMilli(reader.nextLong());
    } else {
      return null;
    }
  }
}
