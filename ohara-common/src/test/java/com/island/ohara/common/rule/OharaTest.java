package com.island.ohara.common.rule;

import com.island.ohara.common.util.CommonUtil;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** import or extends to simplify test */
public abstract class OharaTest extends Assert {

  /**
   * check exception will throws
   *
   * @param c Exception class
   * @param r excuted method
   */
  protected static <T extends Throwable> T assertException(Class<T> c, Runnable r) {
    try {
      r.run();
    } catch (Exception e) {
      Optional.of(e)
          .filter(ex -> !c.isInstance(ex))
          .ifPresent(
              (ex) ->
                  fail(
                      String.format(
                          "Assert ERROR: The %s throws ,but the expected exception is  %s ",
                          c.getName(), ex.getClass().getName())));
      return c.cast(e);
    }
    throw new AssertionError("Assert ERROR: No exception throws");
  }

  /**
   * Use equals method
   *
   * @param s expected string
   * @param r excuted method
   */
  protected static void assertExceptionMsgEquals(String s, Runnable r) {
    Exception e = assertException(Exception.class, r);
    if (e == null) throw new NullPointerException("No exception?");
    if (e.getMessage() == null) throw new NullPointerException("No error message in " + e);
    if (!s.equals(e.getMessage()))
      fail(
          String.format(
              "Assert ERROR: exception msg \"%s\" ,but the expected msg is \"%s\"",
              e.getMessage(), s));
  }

  /**
   * Use contain method
   *
   * @param s expected string
   * @param r excuted method
   */
  protected static void assertExceptionMsgContain(String s, Runnable r) {
    Exception e = assertException(Exception.class, r);
    if (e == null) throw new NullPointerException("No exception?");
    if (e.getMessage() == null) throw new NullPointerException("No error message in " + e);
    if (!e.getMessage().contains(s))
      fail(
          String.format(
              "Assert ERROR: exception msg \"%s\" ,but the expected msg is \"%s\"",
              e.getMessage(), s));
  }

  public String random() {
    return CommonUtil.randomString();
  }

  protected final Logger logger = LoggerFactory.getLogger(OharaTest.class);

  @Rule public final TestName name = new TestName();

  public String methodName() {
    return name.getMethodName();
  }

  /**
   * Skip all remaining test cases after calling this method.
   *
   * @param message why you want to skip all test cases?
   */
  protected void skipTest(String message) {
    Assume.assumeTrue(message, false);
  }
}
