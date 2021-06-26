package com.mtinge.yuugure.core.adapters;

import com.mtinge.yuugure.core.MoshiFactory;
import com.mtinge.yuugure.data.http.Response;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ResponseAdapter extends JsonAdapter<Response> {
  @Override
  public void toJson(@NotNull JsonWriter writer, Response value) throws IOException {
    if (value == null)
      throw new JsonDataException("Response cannot be null");

    // <Response>
    writer.beginObject();
    {
      writer.name("status").value(value.status);
      writer.name("code").value(value.code);

      // <messages>
      writer.name("messages").beginArray();
      if (value.getMessages() != null) {
        for (String message : value.getMessages()) {
          writer.value(message);
        }
      }
      writer.endArray();
      // </messages>

      // <data>
      writer.name("data").beginArray();
      if (value.getData() != null) {
        var adapter = MoshiFactory.create().adapter(Object.class);
        for (var datum : value.getData()) {
          writer.jsonValue(adapter.toJsonValue(datum));
        }
      }
      writer.endArray();
      // </data>
    }
    writer.endObject();
    // </Response>
  }

  @Override
  public Response fromJson(@NotNull JsonReader reader) throws IOException {
    return null;
  }
}
