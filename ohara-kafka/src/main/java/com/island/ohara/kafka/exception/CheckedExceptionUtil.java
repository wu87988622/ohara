package com.island.ohara.kafka.exception;

/** CheckedException util */
public class CheckedExceptionUtil {

  /**
   * This method will wrap CheckedException to RuntimeException with lambda
   *
   * @param cew a lambda throws Exceptions
   * @return anything
   * @throws RuntimeException
   */
  public static <T> T wrap(CheckedExceptionWrapper<T> cew) throws RuntimeException {
    try {
      return cew.wrap();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
