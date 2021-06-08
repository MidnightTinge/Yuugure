package com.mtinge.RedisMutex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

//!
//! Sloppy port of github:@swarthy/redis-semaphore 's mutex module. Notable grabs are the lua.
//! redis-semaphore, (c) 2018 Alexander Mochalin, https://github.com/swarthy/redis-semaphore/blob/b9a94178c433d76262134de1afa3fdb437d19e2a/LICENSE
//!                                               https://github.com/swarthy/redis-semaphore/tree/b9a94178c433d76262134de1afa3fdb437d19e2a/src/mutex
//!

public class RedisMutex {
  private static final String delIfEqualLua = """
    local key = KEYS[1]
    local identifier = ARGV[1]

    if redis.call('get', key) == identifier then
      return redis.call('del', key)
    end
        """;
  private static final Logger logger = LoggerFactory.getLogger(RedisMutex.class);

  private final String _key;
  private final String _identifier;
  private final JedisPool _pool;
  private AtomicBoolean _released = new AtomicBoolean(true);

  private RedisMutex(String key, JedisPool pool) {
    this._key = key;
    this._identifier = UUID.randomUUID().toString();
    this._pool = pool;
  }

  public boolean tryLock() {
    try (var resource = this._pool.getResource()) {
      var res = resource.set(this._key, this._identifier, new SetParams().nx().px(TimeUnit.MINUTES.toMillis(30)));
      _released.set(res != null && !res.equals("OK"));
      return res != null && res.equals("OK");
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public boolean release() {
    try (var resource = this._pool.getResource()) {
      var res = resource.eval(delIfEqualLua, Collections.singletonList(this._key), Collections.singletonList(this._identifier));
      if (!_released.get()) { // only want to call if we were previously locked
        _released.set(res != null);
      }
      return res != null;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public boolean acquire() {
    try {
      acquireFuture().get();
      return true;
    } catch (Exception e) {
      logger.error(String.format("Failed to acquire a lock for %s", _key), e);
      return false;
    }
  }

  public boolean acquire(long timeout, TimeUnit unit) {
    try {
      acquireFuture().get(timeout, unit);
      return true;
    } catch (TimeoutException te) {
      return false;
    } catch (Exception e) {
      logger.error(String.format("Failed to acquire a lock for %s", _key), e);
      return false;
    }
  }

  public Future<Boolean> acquireFuture() {
    return new Future<Boolean>() {
      private boolean _cancelled, _running, _finished;
      private final Object _monitor = new Object();

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        if (_cancelled || _finished) return true;
        if (_running && !mayInterruptIfRunning) return false;
        return (_cancelled = _finished = true);
      }

      @Override
      public boolean isCancelled() {
        return _cancelled;
      }

      @Override
      public boolean isDone() {
        return _finished;
      }

      @Override
      public Boolean get() throws InterruptedException, ExecutionException {
        if (_cancelled || _running || _finished) return null;
        while (true) {
          if (Thread.interrupted()) {
            throw new InterruptedException();
          } else if (_cancelled || _finished) {
            return false;
          }

          if (tryLock()) {
            return true;
          }
          try {
            synchronized (_monitor) {
              _monitor.wait(1);
            }
          } catch (Exception e) {
            logger.error("Failed to sleep", e);
            break;
          }
        }
        return false;
      }

      @Override
      public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        var target = System.currentTimeMillis() + unit.toMillis(timeout);

        if (_cancelled || _running || _finished) return null;
        while (System.currentTimeMillis() < target) {
          if (Thread.interrupted()) {
            throw new InterruptedException();
          } else if (_cancelled || _finished) {
            return false;
          }

          if (tryLock()) {
            return true;
          }
          try {
            synchronized (_monitor) {
              _monitor.wait(1);
            }
          } catch (Exception e) {
            logger.error("Failed to sleep", e);
            break;
          }
        }
        return false;
      }
    };
  }

  public static RedisMutex getMutex(String key, JedisPool pool) {
    return new RedisMutex(key, pool);
  }

  public static boolean forceUnlock(String key, JedisPool pool) {
    throw new Error("ENOIMPL");
  }

}
