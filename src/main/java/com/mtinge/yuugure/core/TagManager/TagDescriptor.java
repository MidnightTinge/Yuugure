package com.mtinge.yuugure.core.TagManager;

import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@ToString
public class TagDescriptor {
  public final TagCategory category;
  public final String name;

  public TagDescriptor(@NotNull TagCategory category, @NotNull String name) {
    this.category = Objects.requireNonNull(category);
    this.name = Objects.requireNonNull(name);
  }

  /**
   * Parse a TagDescriptor from the "name:type" format.
   *
   * @param input The text to parse.
   *
   * @return The parsed TagDescriptor if valid, null otherwise.
   */
  public static TagDescriptor parse(String input) {
    int iof = input.indexOf(':');
    if (iof >= 0) {
      try {
        var category = TagCategory.valueOf(input.substring(0, iof).toUpperCase().trim());
        var name = input.substring(iof + 1);

        return new TagDescriptor(category, name);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return null;
  }

  public boolean equals(Object other) {
    if (other instanceof TagDescriptor) {
      var td = ((TagDescriptor) other);
      return td.category.equals(category) && td.name.equals(name);
    }

    return false;
  }

  public int hashCode() {
    return Objects.hash(category.getName(), name);
  }
}
