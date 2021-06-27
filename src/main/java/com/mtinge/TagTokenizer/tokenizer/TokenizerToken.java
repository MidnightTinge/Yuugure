package com.mtinge.TagTokenizer.tokenizer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;

public class TokenizerToken {
  public enum Type {
    GROUP,
    TERM
  }

  public final TokenizerToken.Type type;
  public final TermModifier modifier;
  @Nullable
  public final String value;
  @Nullable
  public final LinkedList<TokenizerToken> children;

  public TokenizerToken(Type type, TermModifier modifier, @NotNull String value) {
    this.type = type;
    this.modifier = modifier;
    this.value = value;
    this.children = null;
  }

  public TokenizerToken(Type type, TermModifier modifier, @NotNull LinkedList<TokenizerToken> children) {
    this.type = type;
    this.modifier = modifier;
    this.value = null;
    this.children = children;
  }
}
