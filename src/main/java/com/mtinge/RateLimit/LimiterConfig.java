package com.mtinge.RateLimit;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter(AccessLevel.PACKAGE)
@Accessors(fluent = true)
public class LimiterConfig {
  private LimiterRule rule;
  @Setter
  private LimiterType type;
  @Setter
  private String prefix;

  public LimiterConfig(LimiterRule rule) {
    this.rule = rule;
  }

  public static LimiterConfig of(LimiterRule rule) {
    return new LimiterConfig(rule);
  }

}
