/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.common.rule;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestOharaTest extends OharaTest {

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
}
