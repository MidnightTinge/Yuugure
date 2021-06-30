package com.mtinge.TagTokenizer.tokenizer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;

public class TagToken {
  public enum Type {
    GROUP,
    TERM
  }

  public final TagToken.Type type;
  public final TermModifier modifier;
  @Nullable
  public final String value;
  @Nullable
  public final LinkedList<TagToken> children;

  public TagToken(Type type, TermModifier modifier, @NotNull String value) {
    this.type = type;
    this.modifier = modifier;
    this.value = value;
    this.children = null;
  }

  public TagToken(Type type, TermModifier modifier, @NotNull LinkedList<TagToken> children) {
    this.type = type;
    this.modifier = modifier;
    this.value = null;
    this.children = children;
  }
}
