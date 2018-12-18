package com.island.ohara.exception;

/**
 * It supply lambda to throw exception
 *
 * @param <T>
 */
@FunctionalInterface
public interface CheckedExceptionWrapper {
  void wrap() throws Exception;
}
