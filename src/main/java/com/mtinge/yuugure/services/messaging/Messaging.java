package com.mtinge.yuugure.services.messaging;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.MoshiFactory;
import com.mtinge.yuugure.core.PrometheusMetrics;
import com.mtinge.yuugure.data.processor.ProcessorResult;
import com.mtinge.yuugure.services.IService;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.util.Strings;
import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.io.BasicOutputBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Accessors(fluent = true)
public class Messaging implements IService {
  /* INCOMING PAYLOADS */
  public static final byte DEQUEUE_REQUEST = 1;

  /* OUTGOING PAYLOADS */
  public static final byte NO_WORK = 0;
  public static final byte ERRORED = 100;
  public static final byte NOT_ACCEPTABLE = 127;
  public static final String TOPIC_UPLOAD = "NEW_UPLOAD";

  private static final Logger logger = LoggerFactory.getLogger(Messaging.class);
  private static final Object dqMonitor = new Object();

  private final AtomicBoolean shutdownRequested;
  @Getter
  private ZContext context;
  private ZMQ.Socket internalSocket;
  private ZMQ.Socket externalSocket;
  private Thread supplierThread;
  private Thread consolidatorThread;
  private Thread externalThread;

  /* PROMETHEUS STATS */
  private final AtomicInteger jobsAlive;

  public Messaging() {
    shutdownRequested = new AtomicBoolean(false);
    jobsAlive = new AtomicInteger(0);
  }

  @Override
  public void init() throws Exception {
    context = new ZContext();

    supplierThread = new Thread(() -> {
      // TODO yuugure is expected to run on a single box, so it is also expected that sysadmins will
      //      be able to firewall the internal port. that said, we should probably add curve
      //      authentication to lock it down even more.
      internalSocket = context.createSocket(SocketType.REP);
      internalSocket.bind(App.config().zeromq.bind.internalSupplier);

      while (!shutdownRequested.get()) {
        byte[] request = internalSocket.recv(0);

        if (request != null) {
          // note: leaving this as a switch for future expansion.
          switch (request[0]) {
            case DEQUEUE_REQUEST -> {
              synchronized (dqMonitor) {
                var dequeued = App.database().jdbi().withHandle(App.database().processors::dequeue);
                if (!dequeued.isSuccess() || dequeued.getResource() == null) {
                  internalSocket.send(new byte[]{NO_WORK});
                } else {
                  try {
                    var bob = new BasicOutputBuffer();
                    var writer = new BsonBinaryWriter(bob);
                    dequeued.getResource().writeTo(writer);

                    try (var bos = new ByteArrayOutputStream()) {
                      bob.pipe(bos);
                      internalSocket.send(bos.toByteArray());
                    }
                    PrometheusMetrics.MP_JOBS_ALIVE.set(jobsAlive.incrementAndGet());
                    PrometheusMetrics.MP_JOBS_STARTED.inc();
                  } catch (Exception e) {
                    logger.error("Failed to send ProcessableUpload.", e);
                    internalSocket.send(new byte[]{ERRORED});
                  }
                }
              }
            }
            default -> {
              logger.warn("unhandled packet: {}", request[0]);
              internalSocket.send(new byte[]{NOT_ACCEPTABLE});
            }
          }
        } // else: reply was null, nothing received in the timeout window
      }
    }, "yuugure-zmq-supplier");

    // TODO I need to write a proper ROUTER-DEALER to handle heartbeat/etc but for now this works
    //      as a POC of the system to show the pub/sub/push/pull pieces working in tandem.
    consolidatorThread = new Thread(() -> {
      var pull = context.createSocket(SocketType.PULL);
      if (pull.bind(App.config().zeromq.bind.internalConsolidator)) {
        while (!shutdownRequested.get()) {
          var recvd = pull.recv(0);
          if (recvd != null) {
            ProcessorResult result = null;
            try {
              result = ProcessorResult.readFrom(new BsonBinaryReader(ByteBuffer.wrap(recvd)));
            } catch (Exception e) {
              logger.error("Failed to process a pulled response.", e);
            }

            if (result != null) {
              PrometheusMetrics.MP_JOBS_ALIVE.set(jobsAlive.decrementAndGet());
              PrometheusMetrics.MP_JOBS_FINISHED.inc();

              final ProcessorResult _res = result;
              var handleRes = App.database().jdbi().withHandle(handle -> {
                handle.begin();

                var res = App.database().processors.handleResult(_res, handle);
                if (res.isSuccess()) {
                  handle.commit();
                } else {
                  handle.rollback();
                }

                return res;
              });

              if (handleRes.isSuccess()) {
                logger.info("Marked dequeued item {} complete.", result.dequeued().queueItem.id);
              } else {
                logger.warn("Failed to handle result for item {}. Errors:\n{}", result.dequeued().queueItem.id, Strings.join(handleRes.getErrors(), '\n'));
              }
            }
          }
        }
      }
    }, "yuugure-zmq-consolidator");

    externalThread = new Thread(() -> {
      externalSocket = context.createSocket(SocketType.PUB);
      externalSocket.bind(App.config().zeromq.bind.broadcast);
    }, "yuugure-zmq-external");
  }

  @Override
  public void start() throws Exception {
    supplierThread.start();
    consolidatorThread.start();
    externalThread.start();
  }

  @Override
  public void stop() throws Exception {
    shutdownRequested.set(true);
    context.destroy();
    supplierThread.join();
    consolidatorThread.join();
    externalThread.join();
  }

  /**
   * Publish a payload to a topic. All subscribed sockets will receive the payload.
   *
   * @param topic The topic to publish to.
   * @param payload The payload to publish. If it is not a string, we attempt to convert to JSON.
   *   If that fails, we fallback to {@link String#valueOf}
   */
  public void publish(String topic, Object payload) {
    String toSend;
    try {
      toSend = MoshiFactory.create().adapter(Object.class).toJson(payload);
    } catch (Exception e) {
      toSend = String.valueOf(payload);
    }

    externalSocket.sendMore(topic);
    externalSocket.send(toSend);
  }
}
