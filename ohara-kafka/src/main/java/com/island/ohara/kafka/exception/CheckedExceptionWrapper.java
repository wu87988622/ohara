package com.island.ohara.kafka.exception;

/**
 * It supply lambda to throw exception
 *
 * @param <T>
 */
@FunctionalInterface
public interface CheckedExceptionWrapper<T> {
  T wrap() throws Exception;
}
