package com.mtinge.yuugure.core;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.hotspot.DefaultExports;

public class PrometheusMetrics {
  // System metrics
  static {
    DefaultExports.initialize();
  }

  // HTTP
  public static final Counter HTTP_REQUESTS_TOTAL = Counter.build().namespace("yuugure_http").name("requests_total").labelNames("method").help("Total number of HTTP requests.").register();
  public static final Counter HTTP_REQUESTS_AUTHED_TOTAL = Counter.build().namespace("yuugure_http").name("requests_authed_total").help("Total number of authed HTTP requests by method.").register();
  public static final Counter HTTP_REQUESTS_UNAUTHED_TOTAL = Counter.build().namespace("yuugure_http").name("requests_unauthed_total").help("Total number of unauthed HTTP requests by method.").register();
  public static final Histogram HTTP_REQUEST_TIMING = Histogram.build().namespace("yuugure_http").name("request_timing").labelNames("method").help("The timing of requests in seconds.").register();

  // WebSocket
  public static final Counter WS_CONNECTIONS_TOTAL = Counter.build().namespace("yuugure_ws").name("connections_total").help("The total number of WS connections.").register();
  public static final Counter WS_CONNECTIONS_AUTHED_TOTAL = Counter.build().namespace("yuugure_ws").name("authed_connections_total").help("The total number of authenticated WS connections.").register();
  public static final Counter WS_CONNECTIONS_UNAUTHED_TOTAL = Counter.build().namespace("yuugure_ws").name("unauthed_connections_total").help("The total number of unauthenticated WS connections.").register();

  public static final Counter WS_PACKETS_TOTAL = Counter.build().namespace("yuugure_ws").name("packets_total").help("The total number of WS packets.").register();
  public static final Counter WS_PACKETS_TYPED_TOTAL = Counter.build().namespace("yuugure_ws").name("packets_typed_total").labelNames("type").help("The total number of typed WS packets.").register();
  public static final Counter WS_PACKETS_UNHANDLED_TOTAL = Counter.build().name("yuugure_ws").name("packets_unhandled_total").labelNames("type").help("The total number of unhandled packets.").register();
  public static final Counter WS_PACKETS_INVALID_TOTAL = Counter.build().namespace("yuugure_ws").name("packets_invalid_total").help("The total number of invalid packets seen.").register();
  public static final Counter WS_PACKETS_ERRORED_TOTAL = Counter.build().namespace("yuugure_ws").name("packets_errored_total").labelNames("type").help("The total number of valid packets that errored.").register();

  public static final Counter WS_PACKETS_HANDLED_TOTAL = Counter.build().namespace("yuugure_ws").name("packets_handled_total").labelNames("type").help("The total number of valid packets that were handled successfully.").register();

  public static final Gauge WS_CONNECTIONS_TOTAL_CURRENT = Gauge.build().namespace("yuugure_ws").name("connections_total_current").help("The total number of active connections.").register();
  public static final Gauge WS_CONNECTIONS_AUTHED_CURRENT = Gauge.build().namespace("yuugure_ws").name("connections_authed_current").help("The total number of authenticated connections.").register();

  // Uploads
  public static final Counter UPL_TOTAL = Counter.build().namespace("yuugure_upl").name("uploads_total").help("The total number of successful uploads.").register();

  // Media Processor
  public static final Gauge MP_JOBS_ALIVE = Gauge.build().namespace("yuugure_mp").name("jobs_alive").help("The total number of dequeued MediaProcessor jobs.").register();
  public static final Counter MP_JOBS_STARTED = Counter.build().namespace("yuugure_mp").name("jobs_started").help("The total number of jobs started.").register();
  public static final Counter MP_JOBS_FINISHED = Counter.build().namespace("yuugure_mp").name("jobs_finished").help("The total number of jobs completed.").register();

  // ETag Cache
  public static final Counter ETAG_CACHE_HITS = Counter.build().namespace("yuugure_etag_cache").name("hits").help("The total number of cache hits on the ETag cache.").register();
  public static final Counter ETAG_CACHE_MISSES = Counter.build().namespace("yuugure_etag_cache").name("misses").labelNames("reason").help("The total number of cache misses on the ETag cache.").register();

}
