package com.mtinge.yuugure.services.redis;

import com.mtinge.RedisMutex.RedisMutex;
import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.Utils;
import com.mtinge.yuugure.data.postgres.DBAccount;
import com.mtinge.yuugure.services.IService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.net.URI;
import java.time.Duration;

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

  public String getConfirmToken(DBAccount account) {
    try (var rsrc = jedisPool.getResource()) {
      var token = Utils.token(16);
      rsrc.set(token, String.valueOf(account.id), SetParams.setParams().px(Duration.ofMinutes(30).toMillis()));

      return token;
    }
  }

  public boolean confirmToken(String token, DBAccount account, boolean consume) {
    try (var rsrc = jedisPool.getResource()) {
      var fetched = rsrc.get(token);
      var matches = fetched != null && fetched.equals(String.valueOf(account.id));
      if (matches && consume) {
        rsrc.del(token);
      }

      return matches;
    }
  }

  public JedisPool jedis() {
    return jedisPool;
  }

  public RedisMutex getMutex(String name) {
    return RedisMutex.getMutex(name, jedisPool);
  }
}
