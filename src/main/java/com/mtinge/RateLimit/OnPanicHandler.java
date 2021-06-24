package com.mtinge.RateLimit;

import java.net.InetAddress;

public interface OnPanicHandler {
  void onPanic(LimiterRule rule, InetAddress ip);
}
