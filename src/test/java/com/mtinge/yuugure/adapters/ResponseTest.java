package com.mtinge.yuugure.adapters;

import com.mtinge.yuugure.core.adapters.ResponseAdapter;
import com.mtinge.yuugure.data.http.Response;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import io.undertow.util.StatusCodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResponseTest {
  private final JsonAdapter<Response> ada;

  public ResponseTest() {
    var moshi = new Moshi.Builder()
      .add(Response.class, new ResponseAdapter())
      .build();
    this.ada = moshi.adapter(Response.class);
  }

  @Test
  @DisplayName("Converts to json")
  public void toJson() {
    assertEquals("{\"status\":\"OK\",\"code\":200,\"messages\":[],\"data\":[]}", this.ada.toJson(Response.good()));
  }

  @Test
  @DisplayName("Converts to json with correct data")
  public void toJsonWithData() {
    var expected = "{\"status\":\"OK\",\"code\":200,\"messages\":[],\"data\":[0,1]}";
    assertEquals(expected, this.ada.toJson(Response.good().addData(0).addData(1)));
  }

  @Test
  @DisplayName("addAll adds to the existing data instead of injecting a new array")
  void addAllAddsToExisting() {
    var expected = "{\"status\":\"OK\",\"code\":200,\"messages\":[],\"data\":[0,1,2]}";
    List<Object> arr = List.of(0, 1, 2);
    assertEquals(expected, this.ada.toJson(Response.good().addAll(arr)));
  }

  @Test
  @DisplayName("Response convenience methods with Iterable datum correctly result in a flat array")
  void convenienceMethodsCallAddAll() {
    var expected = "{\"status\":\"OK\",\"code\":200,\"messages\":[],\"data\":[0,1,2]}";
    List<Object> arr = List.of(0, 1, 2);
    assertEquals(expected, this.ada.toJson(Response.good(arr)));

    expected = "{\"status\":\"Bad Request\",\"code\":400,\"messages\":[],\"data\":[0,1,2]}";
    arr = List.of(0, 1, 2);
    assertEquals(expected, this.ada.toJson(Response.fromCode(StatusCodes.BAD_REQUEST, arr)));
  }

  @Test
  @DisplayName("Constructor differentiates between a List and Object datum")
  void constructorHandlesList() {
    var expected = "{\"status\":\"OK\",\"code\":200,\"messages\":[],\"data\":[0,1,2]}";
    List<Object> arr = List.of(0, 1, 2);
    assertEquals(expected, this.ada.toJson(new Response("OK", 200, arr)));
  }

}
