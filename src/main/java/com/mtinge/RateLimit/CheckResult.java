package com.mtinge.RateLimit;

import lombok.ToString;

import java.time.Duration;

@ToString
public class CheckResult {
  public final boolean overLimit;
  public final boolean panicWorthy;
  public final long itemsInWindow;
  public final long windowMaximum;
  public final Duration window;
  public final Duration nextAvailable;

  public CheckResult(boolean overLimit, boolean panicWorthy, long itemsInWindow, long windowMaximum, Duration window, Duration nextAvailable) {
    this.overLimit = overLimit;
    this.panicWorthy = panicWorthy;
    this.itemsInWindow = itemsInWindow;
    this.windowMaximum = windowMaximum;
    this.window = window;
    this.nextAvailable = nextAvailable;
  }
}
