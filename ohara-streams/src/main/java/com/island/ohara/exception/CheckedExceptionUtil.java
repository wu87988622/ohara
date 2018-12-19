package com.island.ohara.exception;

/** CheckedException util */
public class CheckedExceptionUtil {

  /**
   * This method will wrap CheckedException to RuntimeException with lambda
   *
   * @param cew a lambda throws Exceptions
   * @throws RuntimeException
   */
  public static void wrap(CheckedExceptionWrapper cew) throws RuntimeException {
    try {
      cew.wrap();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
