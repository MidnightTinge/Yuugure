package com.mtinge.yuugure.core.adapters;

import com.mtinge.yuugure.core.MoshiFactory;
import com.mtinge.yuugure.data.http.Response;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

public class ResponseAdapter extends JsonAdapter<Response> {
  @Override
  public void toJson(JsonWriter writer, Response value) throws IOException {
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
      writer.name("data").beginObject();
      if (value.getData() != null) {
        var moshi = MoshiFactory.create();
        for (var classListEntry : value.getData().entrySet()) {
          if (classListEntry.getValue() != null) {
            var val = classListEntry.getValue();
            writer.name(classListEntry.getKey());
            writer.beginArray();
            for (Object o : val) {
              writer.jsonValue(moshi.adapter(Object.class).toJsonValue(o));
            }
            writer.endArray();
          }
        }
      }
      writer.endObject();
      // </data>
    }
    writer.endObject();
    // </Response>
  }

  @Override
  public Response fromJson(JsonReader reader) throws IOException {
    return null;
  }
}
