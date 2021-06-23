package com.mtinge.yuugure.data.http;

import io.undertow.util.StatusCodes;

import java.util.*;

public class Response {
  public final String status;
  public final int code;
  private transient List<String> _messages = new ArrayList<String>();
  private transient Map<String, List<Object>> _data = new LinkedHashMap<>();

  public static Response fromCode(int code) {
    return new Response(StatusCodes.getReason(code), code);
  }

  public static Response bad(int code, String reason) {
    return new Response(reason, code);
  }

  public static Response good() {
    return Response.fromCode(StatusCodes.OK);
  }

  public static Response good(String message) {
    return Response.good().addMessage(message);
  }

  public static Response exception() {
    return Response.fromCode(StatusCodes.INTERNAL_SERVER_ERROR).addMessage("An internal server error occurred. Please try again later.");
  }

  public Response(String status, int code) {
    this.status = status;
    this.code = code;
  }

  public <Z> Response addAll(Class<Z> mainType, List<Z> datas) {
    return addAll(mainType.getSimpleName(), datas);
  }

  public <Z> Response addAll(String key, List<Z> datas) {
    if (datas == null || datas.isEmpty()) return this;
    var existing = _data.get(key);
    if (existing != null) {
      existing.addAll(datas);
    } else {
      _data.put(key, new ArrayList<>(datas));
    }

    return this;
  }

  public <Z> Response addData(Class<Z> dataType, Z data) {
    return addData(dataType.getSimpleName(), data);
  }

  public <Z> Response addData(String key, Z data) {
    if (data == null) return this;

    var existing = _data.get(key);
    if (existing == null) {
      _data.put(key, new ArrayList<>(Collections.singleton(data)));
    } else {
      existing.add(data);
    }
    return this;
  }

  public Response addMessage(String message) {
    if (message == null || message.isBlank()) return this;

    _messages.add(message);
    return this;
  }

  public Response addAllMessages(Collection<String> messages) {
    _messages.addAll(messages);

    return this;
  }

  public Response addAllMessages(String[] messages) {
    for (String message : messages) {
      _messages.add(message);
    }

    return this;
  }

  public List<String> getMessages() {
    return _messages;
  }

  public Map<String, List<Object>> getData() {
    return _data;
  }
}
