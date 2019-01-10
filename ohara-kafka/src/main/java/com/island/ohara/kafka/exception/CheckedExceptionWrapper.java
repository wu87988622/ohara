package com.island.ohara.kafka.exception;

/** It supply lambda to throw exception */
@FunctionalInterface
public interface CheckedExceptionWrapper {
  void excute() throws Exception;
}
