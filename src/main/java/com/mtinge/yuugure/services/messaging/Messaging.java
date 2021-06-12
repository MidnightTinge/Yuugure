package com.mtinge.yuugure.services.messaging;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.MoshiFactory;
import com.mtinge.yuugure.services.IService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.concurrent.atomic.AtomicBoolean;

public class Messaging implements IService {
  private static final Logger logger = LoggerFactory.getLogger(Messaging.class);

  private static final String DEQUEUE_REQUEST = "1";

  public static final String TOPIC_UPLOAD = "NEW_UPLOAD";

  AtomicBoolean shutdownRequested;
  ZContext context;
  ZMQ.Socket internalSocket;
  ZMQ.Socket externalSocket;
  Thread internalThread;
  Thread externalThread;

  public Messaging() {
    shutdownRequested = new AtomicBoolean(false);
  }

  @Override
  public void init() throws Exception {
    context = new ZContext();

    internalThread = new Thread(() -> {
      // TODO yuugure is expected to run on a single box, so it is also expected that sysadmins will
      //      be able to firewall the internal port. that said, we should probably add curve
      //      authentication to lock it down even more.
      internalSocket = context.createSocket(SocketType.REP);
      internalSocket.bind(App.config().zeromq.bind.internal);

      while (!shutdownRequested.get()) {
        byte[] reply = internalSocket.recv(0);

        if (reply != null) {
          var recvd = new String(reply, ZMQ.CHARSET);
          logger.debug("received {}", recvd);

          // note: leaving this as a switch for future expansion.
          switch (recvd) {
            case DEQUEUE_REQUEST -> {
              logger.info("got dq request");
            }
            default -> {
              //
            }
          }

        } // else: reply was null, nothing received in the timeout window
      }
    }, "yuugure-zmq-internal");

    externalThread = new Thread(() -> {
      externalSocket = context.createSocket(SocketType.PUB);
      externalSocket.bind(App.config().zeromq.bind.external);
    }, "yuugure-zmq-external");
  }

  @Override
  public void start() throws Exception {
    internalThread.start();
    externalThread.start();
  }

  @Override
  public void stop() throws Exception {
    shutdownRequested.set(true);
    context.destroy();
    internalThread.join();
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
