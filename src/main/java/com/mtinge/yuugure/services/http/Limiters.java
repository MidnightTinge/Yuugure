package com.mtinge.yuugure.services.http;

import com.mtinge.RateLimit.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Duration;

@Getter
@Accessors(fluent = true)
public class Limiters {
  @Getter(AccessLevel.NONE)
  private final LimiterFactory factory;

  private final Limiter reportLimiter;
  private final Limiter commentLimiter;
  private final Limiter uploadLimiter;

  public Limiters(LimiterFactory factory) {
    this.factory = factory;

    this.reportLimiter = factory.newLimiter(
      LimiterConfig.of(LimiterRule.of(Duration.ofSeconds(30), 3).panicAmount(20)).type(LimiterType.SLIDING_WINDOW).prefix("rl:report:")
    );

    this.commentLimiter = factory.newLimiter(
      LimiterConfig.of(LimiterRule.of(Duration.ofMinutes(1), 3).panicAmount(50)).type(LimiterType.SLIDING_WINDOW).prefix("rl:comment:")
    );

    this.uploadLimiter = factory.newLimiter(
      LimiterConfig.of(LimiterRule.of(Duration.ofMinutes(1), 5).panicAmount(50)).type(LimiterType.SLIDING_WINDOW).prefix("rl:upload:")
    );
  }
}
