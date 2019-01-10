package com.island.ohara.kafka.exception;

/**
 * It supply lambda to throw exception
 *
 * @param <T> return anything
 */
@FunctionalInterface
public interface CheckedExceptionWrapperReturn<T> {
  T excute() throws Exception;
}
