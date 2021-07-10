package com.mtinge.yuugure.services.http.handlers;

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.node.Node;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.mtinge.RateLimit.OnPanicHandler;
import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.MoshiFactory;
import com.mtinge.yuugure.core.PrometheusMetrics;
import com.mtinge.yuugure.core.ThreadFactories;
import com.mtinge.yuugure.data.postgres.DBPanicConnection;
import com.squareup.moshi.Moshi;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PanicHandler implements HttpHandler {
  private static final Logger logger = LoggerFactory.getLogger(PanicHandler.class);
  private static final Logger sweepLogger = LoggerFactory.getLogger("PanicHandler-sweep");
  private static final Logger panicLogger = LoggerFactory.getLogger("panics");
  private final OnPanicHandler onPanic;
  private HttpHandler next;
  private ConcurrentRadixTree<Long> blocks;
  public static final TimeUnit BLOCK_UNIT = TimeUnit.HOURS;
  public static final long BLOCK_AMOUNT = 1;

  private final Moshi moshi = MoshiFactory.create();

  public PanicHandler() {
    this.blocks = new ConcurrentRadixTree<>(new DefaultByteArrayNodeFactory());
    this.onPanic = (rule, addr) -> {
      var now = Instant.now();
      var expires = now.plusMillis(BLOCK_UNIT.toMillis(BLOCK_AMOUNT));

      panicLogger.info("{}", moshi.adapter(Object.class).toJson(Map.of("address", addr.getHostAddress(), "rule", rule, "now", now.toEpochMilli())));
      PrometheusMetrics.PANIC_TRIGGERS_TOTAL.inc();

      // put an initial block length so that subsequent connections get 403'd while we're in the db
      this.blocks.putIfAbsent(addr.getHostAddress(), expires.toEpochMilli());

      try {
        var length = App.database().jdbi().withHandle(handle -> {
          boolean rolledBack = false;

          handle.begin();
          try {
            // warning: Access exclusive is a faily heavy lock. We're using it here due to the need
            //          for data atomicity and because it is assumed panics will happen rarely.
            handle.execute("LOCK TABLE panic_connection IN ACCESS EXCLUSIVE MODE");
            var existingConnection = handle.createQuery("SELECT * FROM panic_connection WHERE addr = :addr")
              .bind("addr", addr.getAddress())
              .mapTo(DBPanicConnection.class)
              .findFirst().orElse(null);
            if (existingConnection == null) {
              handle.createUpdate("INSERT INTO panic_connection (addr, timestamp, expires) VALUES (:addr, :timestamp, :expires)")
                .bind("addr", addr.getAddress())
                .bind("timestamp", Timestamp.from(now))
                .bind("expires", Timestamp.from(expires))
                .execute();
            } else {
              var updatedCon = handle.createQuery("UPDATE panic_connection SET timestamp = :timestamp, expires = :expires, hits = hits + 1 WHERE addr = :addr RETURNING *")
                .bind("addr", addr.getAddress())
                .bind("timestamp", Timestamp.from(now))
                .bind("expires", Timestamp.from(expires))
                .mapTo(DBPanicConnection.class)
                .findFirst().orElse(null);

              if (updatedCon != null) {
                if (updatedCon.hits > 1) {
                  return BLOCK_UNIT.toMillis(Double.valueOf(Math.ceil((BLOCK_AMOUNT * 1.25) * updatedCon.hits)).longValue());
                }
              } else {
                logger.warn("Failed to update the panic_connection for addr {}!", addr.getHostAddress());
              }
            }
          } catch (Exception e) {
            rolledBack = true;
            handle.rollback();
            logger.error("Panic query chain failed.", e);
          } finally {
            if (!rolledBack) {
              handle.commit();
            }
          }
          return null;
        });

        if (length != null) {
          var corrected = now.plusMillis(length).toEpochMilli();
          var current = this.blocks.getValueForExactKey(addr.getHostAddress());
          if (current != null) {
            if (current <= corrected) {
              this.blocks.put(addr.getHostAddress(), corrected);
            }
          }
        }
      } catch (Exception e) {
        logger.error("Failed to assign blockLength, using default.", e);
      }
    };

    var executor = Executors.newSingleThreadScheduledExecutor(ThreadFactories.named("PanicHandler-sweep"));
    executor.scheduleAtFixedRate(this::sweep, 1, 1, TimeUnit.HOURS);
  }

  private void _sweep(Node node, Long now, List<String> removed, String path) {
    if (node == null) return;
    for (var n : node.getOutgoingEdges()) {
      _sweep(n, now, removed, path + n.getIncomingEdge());
    }

    if (node.getValue() == null) return;
    if ((((Long) node.getValue()) - now) <= 0) {
      this.blocks.remove(path);
      removed.add(path);
    }
  }

  public LinkedList<String> sweep() {
    var removed = new LinkedList<String>();

    sweepLogger.info("Running...");
    try {
      _sweep(this.blocks.getNode(), Instant.now().toEpochMilli(), removed, "");
      sweepLogger.info("Sweep finished, removed {} items.", removed.size());
    } catch (Exception e) {
      sweepLogger.error("Failed to sweep.", e);
    }

    return removed;
  }

  private void _traverse(Node node, Long now, Map<String, Long> tracked, boolean skipExpired, String path) {
    if (node == null) return;
    for (var n : node.getOutgoingEdges()) {
      _traverse(n, now, tracked, skipExpired, path + n.getIncomingEdge());
    }

    if (node.getValue() == null) return;
    var remaining = (((Long) node.getValue()) - now);
    if (skipExpired) {
      if (remaining > 0) {
        tracked.put(path, remaining);
      }
    } else {
      tracked.put(path, remaining);
    }
  }

  public Map<String, Long> getDenials(boolean all) {
    var ret = new HashMap<String, Long>();
    _traverse(this.blocks.getNode(), Instant.now().toEpochMilli(), ret, !all, "");

    return ret;
  }

  public boolean allow(String ip) {
    return this.blocks.remove(ip);
  }

  public void deny(String ip, long blockExpiry) {
    this.blocks.put(ip, blockExpiry);
  }

  public PanicHandler wrap(HttpHandler next) {
    this.next = next;
    return this;
  }

  public void reload() {
    logger.info("Reload waiting...");
    App.database().jdbi().withHandle(handle -> {
      boolean rolledBack = false;

      handle.begin();
      try {
        // warning: Access exclusive is a fairly heavy lock. We're using it here due to the need for
        //          data atomicity and because it is assumed panics will happen rarely.
        handle.execute("LOCK TABLE panic_connection IN ACCESS EXCLUSIVE MODE");
        logger.info("Reload started");
        var panics = handle.createQuery("SELECT * FROM panic_connection WHERE expires > now()")
          .mapTo(DBPanicConnection.class)
          .list();

        var toSet = new ConcurrentRadixTree<Long>(new DefaultByteArrayNodeFactory());
        for (var panic : panics) {
          toSet.put(panic.addrAsInet().getHostAddress(), panic.expires.toInstant().toEpochMilli());
        }
        this.blocks = toSet; // only swap the memory once we know we completed successfully

        logger.info("Reloaded successfully. {} entries tracked.", panics.size());
      } catch (Exception e) {
        rolledBack = true;
        handle.rollback();
        logger.error("Reload failed...", e);
      } finally {
        if (!rolledBack) {
          handle.commit();
        }
      }
      return null;
    });
  }

  public OnPanicHandler getPanicHandler() {
    return this.onPanic;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    if (this.next == null)
      throw new IllegalStateException("handleRequest fired before wrap called");
    var now = Instant.now();
    var key = exchange.getAttachment(AddressHandler.ATTACHMENT_KEY).getHostAddress();
    var unblockAt = this.blocks.getValueForExactKey(key);
    if (unblockAt != null) {
      if ((unblockAt - now.toEpochMilli()) <= 0) {
        this.blocks.remove(key);
      } else {
        exchange.setStatusCode(StatusCodes.FORBIDDEN);
        if (App.isDebug()) {
          exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "application/json");
          exchange.getResponseSender().send(moshi.adapter(Object.class).toJson(Map.of("remaining", Duration.ofMillis(unblockAt - now.toEpochMilli()))));
        } else {
          exchange.endExchange();
        }
      }
    }
    if (!exchange.isComplete())
      this.next.handleRequest(exchange);
  }
}
