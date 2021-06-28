package com.mtinge.yuugure.data.elastic;

import lombok.AllArgsConstructor;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.document.DocumentField;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
public final class EUpload {
  public final int id;
  public final List<Integer> tags;

  @Nullable
  public static EUpload fromFields(Map<String, DocumentField> fields) {
    if (!fields.containsKey("id")) {
      return null;
    }
    if (!fields.containsKey("tags")) {
      return null;
    }

    Integer id = fields.get("id").getValue();
    List<Integer> tags = fields.get("tags").getValues().stream()
      .filter(o -> o instanceof Integer)
      .map(o -> (Integer) o)
      .collect(Collectors.toList());

    return new EUpload(id, tags);
  }
}
