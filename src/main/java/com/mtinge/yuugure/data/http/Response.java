package com.mtinge.yuugure.data.http;

import io.undertow.util.StatusCodes;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class Response {
  public final int code;
  public final String status;
  private transient final LinkedList<String> messages = new LinkedList<>();
  private transient final LinkedList<Object> data = new LinkedList<>();

  public Response(String status, int code) {
    this.code = code;
    this.status = status;
  }

  @SuppressWarnings("unchecked")
  public Response(String status, int code, @NotNull Object datum) {
    this.code = code;
    this.status = status;
    if (datum instanceof List) {
      addAll(((List) datum));
    } else {
      addData(datum);
    }
  }

  public static  Response good() {
    return new Response("OK", 200);
  }

  public static  Response good(Object datum) {
    return new Response("OK", 200, datum);
  }

  public static  Response good(List<Object> data) {
    return new Response("OK", 200).addAll(data);
  }

  public static  Response fromCode(int code) {
    return new Response(StatusCodes.getReason(code), code);
  }

  public static  Response fromCode(int code, Object datum) {
    return new Response(StatusCodes.getReason(code), code, datum);
  }

  public static  Response fromCode(int code, List<Object> datum) {
    return new Response(StatusCodes.getReason(code), code, datum);
  }

  public Response addData(Object datum) {
    this.data.add(datum);
    return this;
  }

  public Response addAll(List<?> data) {
    this.data.addAll(data);
    return this;
  }

  public Response addMessage(String message) {
    this.messages.addLast(message);
    return this;
  }

  public LinkedList<Object> getData() {
    return data;
  }

  public LinkedList<String> getMessages() {
    return messages;
  }
}
