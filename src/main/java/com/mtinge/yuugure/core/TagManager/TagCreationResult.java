package com.mtinge.yuugure.core.TagManager;

import com.mtinge.yuugure.data.postgres.DBTag;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;


@AllArgsConstructor
public class TagCreationResult {
  @NotNull
  public final List<DBTag> tags;
  @NotNull
  public final List<String> messages;

  public void addTag(DBTag tag) {
    this.tags.add(tag);
  }

  public void addMessage(String message) {
    this.messages.add(message);
  }

  public static TagCreationResult forTags(List<DBTag> tags) {
    return new TagCreationResult(tags, List.of());
  }

  public static TagCreationResult forMessages(List<String> messages) {
    return new TagCreationResult(List.of(), messages);
  }
}
