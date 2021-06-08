package com.mtinge.yuugure.services;

public interface IService {
  void init() throws Exception;

  void start() throws Exception;

  default void stop() throws Exception {
    // stub
  }
}
