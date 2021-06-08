package com.mtinge.yuugure.core;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadFactories {
  private static final AtomicInteger _aiPrefix = new AtomicInteger(0);

  public static ThreadFactory prefixed(String prefix) {
    return r -> new Thread(r, prefix + _aiPrefix.incrementAndGet());
  }

  public static ThreadFactory named(String name) {
    return r -> new Thread(r, name);
  }
}
