package com.mtinge.yuugure.core.TagManager;

import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@ToString
public class TagDescriptor {
  private static final Logger logger = LoggerFactory.getLogger(TagDescriptor.class);

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
   * @param defaultUserland If there is no category we'll force it to {@link TagCategory#USERLAND}
   *   when true.
   *
   * @return The parsed TagDescriptor if valid, null otherwise.
   */
  public static TagDescriptor parse(@NotNull String input, boolean defaultUserland) {
    TagCategory category = null;
    String name = null;
    int iof = input.indexOf(':');
    if (iof >= 0 && iof < input.length() - 1) {
      var cat = input.substring(0, iof).toUpperCase().trim();
      name = input.substring(iof + 1);
      try {
        category = TagCategory.valueOf(cat);
      } catch (IllegalArgumentException iae) {
        // ignored, assumed a tag like "16:9_aspect_ratio"
        if (defaultUserland) {
          category = TagCategory.USERLAND;
        }
      }
    } else if (defaultUserland) {
      name = input;
      category = TagCategory.USERLAND;
    }

    return category == null ? null : new TagDescriptor(category, name);
  }

  /**
   * Parse a TagDescriptor from the "name:type" format.
   *
   * @param input The text to parse.
   *
   * @return The parsed TagDescriptor if valid, null otherwise.
   */
  public static TagDescriptor parse(String input) {
    return parse(input, false);
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
