package com.mtinge.yuugure.core.TagManager;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TagDescriptor {
  public final String name;
  public final TagType type;

  public TagDescriptor(@NotNull String name, @NotNull TagType type) {
    this.name = Objects.requireNonNull(name);
    this.type = Objects.requireNonNull(type);
  }

  /**
   * Parse a TagDescriptor from the "name:type" format.
   *
   * @param input The text to parse.
   *
   * @return The parsed TagDescriptor if valid, null otherwise.
   */
  public static TagDescriptor parse(String input) {
    int iof = input.lastIndexOf(':');
    if (iof >= 0) {
      try {
        var name = input.substring(0, iof);
        var type = TagType.valueOf(input.substring(iof + 1).toUpperCase().trim());

        return new TagDescriptor(name, type);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return null;
  }
}
