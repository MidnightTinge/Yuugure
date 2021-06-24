package com.mtinge.RateLimit;

import lombok.Getter;
import lombok.Setter;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Limiter {
  @Getter
  @Setter
  private String prefix;
  @Getter
  @Setter
  private LimiterRule rule;
  @Getter
  private LimiterType type;

  private final Object _lock = new Object();
  private final LimiterFactory factory;

  Limiter(LimiterConfig config, LimiterFactory factory) {
    this.rule = config.rule();
    this.type = config.type();
    this.prefix = config.prefix();
    this.factory = factory;
  }

  public CheckResult check(InetAddress ip) {
    return check(ip, true);
  }

  @SuppressWarnings("unchecked")
  public CheckResult check(InetAddress ip, boolean increment) {
    long now = Instant.now().toEpochMilli();
    var item = ip.getHostAddress();

    try (var rsrc = factory.pool().getResource()) {
      var key = formatKey(item);
      int count = 0;
      long nextAvailable = 0;

      synchronized (_lock) {
        switch (type) {
          case BUCKET -> {
            var evald = rsrc.eval(increment ? Scripts.Bucket.INCREMENT : Scripts.Bucket.COMPUTE, List.of(item), List.of(String.valueOf(rule.window().toMillis())));
            if (evald instanceof List) {
              var result = (List<Long>) evald;
              if (!result.isEmpty()) {
                count = result.get(0).intValue();
                nextAvailable = count > rule.maximum() ? result.get(1) : 0;
              }
            }
          }
          case SLIDING_WINDOW -> {
            var eval = rsrc.eval(increment ? Scripts.SlidingWindow.INCREMENT : Scripts.SlidingWindow.COMPUTE, Collections.singletonList(key), Arrays.asList(String.valueOf(now), String.valueOf(rule.window().toMillis())));
            if (eval instanceof ArrayList) {
              count = increment ? 1 : 0;

              var casted = (ArrayList<String>) eval;
              if (!casted.isEmpty()) {
                if (increment) {
                  casted.add(String.valueOf(now));
                }
                count = casted.size();

                if (count > rule.maximum()) {
                  // set nextAvailable equal to the number of ms it will take for the tail of the
                  // window to shrink enough that at least one entry is available.
                  nextAvailable = rule.window().toMillis() - (now - Long.parseLong(casted.get(casted.size() - rule.maximum())));
                }
              }
            }
          }
        }
      }

      if (rule.isPanicWorthy(count)) {
        factory.panic(rule, ip);
      }

      return new CheckResult(count > rule.maximum(), rule.isPanicWorthy(count), count, rule.maximum(), rule.window(), Duration.ofMillis(nextAvailable));
    }
  }

  private String formatKey(String key) {
    return (this.prefix == null ? "" : this.prefix) + key;
  }

  private static final class Scripts {
    public final static class SlidingWindow {
      public static final String INCREMENT = """
        local key = KEYS[1]

        local now = ARGV[1]
        local window = ARGV[2]

        local returnable = 0

        redis.call('ZREMRANGEBYSCORE', key, 0, (now - window))
        returnable = redis.call('ZRANGE', key, 0, -1)
        redis.call('ZADD', key, now, now)
        redis.call('PEXPIRE', key, window)

        return returnable
        """;

      public static final String COMPUTE = """
        local key = KEYS[1]

        local now = ARGV[1]
        local window = ARGV[2]

        local returnable = 0

        if (redis.call('EXISTS', key)) == 1 then
            redis.call('ZREMRANGEBYSCORE', key, 0, (now - window))
            returnable = redis.call('ZRANGE', key, 0, -1)
        end

        return returnable
        """;
    }

    public static final class Bucket {
      public static final String INCREMENT = """
        local key = KEYS[1]
        local timeout = ARGV[1]

        local calls = redis.call('INCR', key)
        if (calls == 1) then
          redis.call('PEXPIRE', key, timeout)
        end
        local expires = redis.call('PTTL', key)

        return {calls, expires}
        """;

      public static final String COMPUTE = """
        local key = KEYS[1]
        local timeout = ARGV[1]
        local calls = 0
        local expires = 0

        if redis.call('EXISTS', key) then
          calls = redis.call('INCRBY', key, 0)
          expires = redis.call('PTTL', key)
        else
          calls = 0
          expires = 0
        end

        return {calls, expires}
        """;
    }
  }
}
