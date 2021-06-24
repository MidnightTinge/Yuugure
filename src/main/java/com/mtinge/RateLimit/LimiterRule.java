package com.mtinge.RateLimit;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.Duration;

@Getter
@Setter
@ToString
@Accessors(fluent = true)
public class LimiterRule {
  private Duration window;
  private int maximum;
  private Integer panicAmount;

  public LimiterRule(Duration window, int maximum) {
    this(window, maximum, null);
  }

  public LimiterRule(Duration window, int maximum, Integer panicAmount) {
    this.window = window;
    this.maximum = maximum;
    this.panicAmount = panicAmount;
  }

  public boolean isPanicWorthy(int amount) {
    if (panicAmount == null) return false;
    return amount >= panicAmount;
  }

  public static LimiterRule of(Duration window, int maximum) {
    return new LimiterRule(window, maximum);
  }
}
