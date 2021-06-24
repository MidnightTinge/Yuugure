package com.mtinge.yuugure.adapters;

import com.mtinge.yuugure.core.adapters.ResponseAdapter;
import com.mtinge.yuugure.data.http.Response;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResponseTest {
  private final Moshi moshi;
  private final JsonAdapter<Response> ada;

  public ResponseTest() {
    this.moshi = new Moshi.Builder()
      .add(Response.class, new ResponseAdapter())
      .build();
    this.ada = this.moshi.adapter(Response.class);
  }

  @Test
  @DisplayName("Converts to json")
  public void toJson() {
    assertEquals("{\"status\":\"OK\",\"code\":200,\"messages\":[],\"data\":{}}", this.ada.toJson(Response.good()));
  }

}
