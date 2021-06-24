package com.mtinge.RateLimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

public class LimiterFactory {
  private static final Logger logger = LoggerFactory.getLogger(LimiterFactory.class);
  private final OnPanicHandler panicHandler;
  private final JedisPool jedisPool;
  private final AtomicInteger numFails = new AtomicInteger(0);

  public LimiterFactory(OnPanicHandler panicHandler, JedisPool jedisPool) {
    this.panicHandler = panicHandler;
    this.jedisPool = jedisPool;
  }

  public Limiter newLimiter(LimiterConfig config) {
    return new Limiter(config, this);
  }

  protected synchronized void panic(LimiterRule rule, InetAddress ip) {
    try {
      this.panicHandler.onPanic(rule, ip);
      numFails.set(0);
    } catch (Exception e) {
      if (numFails.incrementAndGet() >= 3) {
        throw e;
      }
      logger.error("Failed to run panic handler.", e);
    }
  }

  protected JedisPool pool() {
    return this.jedisPool;
  }
}
