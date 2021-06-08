package com.mtinge.yuugure.services.redis;

import com.mtinge.RedisMutex.RedisMutex;
import com.mtinge.yuugure.App;
import com.mtinge.yuugure.services.IService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.net.URI;

public class Redis implements IService {
  private static final Logger logger = LoggerFactory.getLogger(Redis.class);
  private JedisPool jedisPool;

  @Override
  public void init() throws Exception {
    this.jedisPool = new JedisPool(new URI(App.config().redis.url));
  }

  @Override
  public void start() throws Exception {
    try (var res = this.jedisPool.getResource()) {
      res.ping();
    }
    logger.info("Redis started");
  }

  public JedisPool jedis() {
    return jedisPool;
  }

  public RedisMutex getMutex(String name) {
    return RedisMutex.getMutex(name, jedisPool);
  }
}
