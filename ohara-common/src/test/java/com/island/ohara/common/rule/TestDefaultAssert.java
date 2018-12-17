package com.island.ohara.common.rule;

import org.junit.Test;

public class TestDefaultAssert extends SmallTest {

  @Test
  public void TestException() {
    assertException(
        IllegalArgumentException.class,
        () -> {
          throw new IllegalArgumentException("test");
        });

    assertException(
        ArithmeticException.class,
        () -> {
          throw new ArithmeticException("test");
        });
  }

  @Test(expected = AssertionError.class)
  public void TestExceptionError() {
    assertException(
        IllegalArgumentException.class,
        () -> {
          //                not match exception
          throw new ArithmeticException("test");
        });
    throw new RuntimeException("assertException didn't fail , normally can't see this msg");
  }

  @Test(expected = AssertionError.class)
  public void TestExceptionError2() {
    assertException(IllegalArgumentException.class, () -> {});
    throw new RuntimeException("assertException didn't fail , normally can't see this msg");
  }

  @Test
  public void TestExceptionCompare() {
    Exception e =
        assertException(
            RuntimeException.class,
            () -> {
              throw new RuntimeException(new ArithmeticException("test"));
            });

    assertEquals(e.getClass(), RuntimeException.class);
    assertEquals(e.getCause().getClass(), ArithmeticException.class);
    assertEquals(e.getCause().getMessage(), "test");
  }

  @Test
  public void TestExceptionMsg() {
    assertExceptionMsgEquals(
        "test",
        () -> {
          throw new IllegalArgumentException("test");
        });

    assertExceptionMsgEquals(
        "test22",
        () -> {
          throw new RuntimeException("test22");
        });

    assertExceptionMsgEquals(
        "test333",
        () -> {
          throw new ArithmeticException("test333");
        });
  }

  @Test(expected = AssertionError.class)
  public void TestExceptionMsgError() {
    assertExceptionMsgEquals(
        "test333",
        () -> {
          //            No exception
        });
    throw new RuntimeException("assertException didn't fail , normally can't see this msg");
  }

  @Test
  public void TestExceptionMsgContain() {
    assertExceptionMsgContain(
        "admin",
        () -> {
          throw new IllegalArgumentException("My name is admin");
        });

    assertExceptionMsgContain(
        "HAHA",
        () -> {
          throw new RuntimeException("My name is Chia ,HAHAHA");
        });

    assertExceptionMsgContain(
        "is",
        () -> {
          throw new ArithmeticException("My name is Jack");
        });
  }

  @Test(expected = AssertionError.class)
  public void TestExceptionMsgContainError() {
    assertExceptionMsgContain(
        "test333",
        () -> {
          //            No exception
        });
    throw new RuntimeException("assertException didn't fail , normally can't see this msg");
  }
}
