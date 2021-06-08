package com.mtinge.RedisMutex;

public interface RedisMutexCallback {
  void acquired(RedisMutex mutex);

  void failed(Error reason);
}
