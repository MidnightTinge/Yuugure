package com.mtinge.yuugure.core.TagManager;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
public class TagSearch {
  public final SearchMode mode;
  @Nullable
  public final String prefix;
  @Nullable
  public final String suffix;

  public static TagSearch prefix(String search) {
    return new TagSearch(SearchMode.PREFIX, search, null);
  }

  public static TagSearch suffix(String search) {
    return new TagSearch(SearchMode.SUFFIX, null, search);
  }

  public static TagSearch middle(String prefix, String suffix) {
    return new TagSearch(SearchMode.MIDDLE, prefix, suffix);
  }

  public static TagSearch wrapped(String wrapped) {
    return new TagSearch(SearchMode.WRAPPED, wrapped, null);
  }
}
