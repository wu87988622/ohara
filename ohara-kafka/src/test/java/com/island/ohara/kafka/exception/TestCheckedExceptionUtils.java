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

package com.island.ohara.kafka.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.island.ohara.common.rule.SmallTest;
import org.junit.Test;

public class TestCheckedExceptionUtils extends SmallTest {

  // checked exception
  private final Exception checkedException = new ClassNotFoundException();

  // unchecked exception
  private final RuntimeException uncheckedException = new ClassCastException();

  // special unique class name
  private class OharaTest0000Exception extends OharaException {
    private static final long serialVersionUID = 1L;

    OharaTest0000Exception(Exception e) {
      super(e);
    }
  }

  private final ExceptionHandler handler =
      ExceptionHandler.creator()
          .add(checkedException.getClass(), OharaTest0000Exception::new)
          .create();

  @Test
  public void TestWrap() {
    Exception e =
        assertException(
            RuntimeException.class,
            () ->
                CheckedExceptionUtils.wrap(
                    () -> {
                      throw checkedException;
                    }));

    assertEquals(e.getCause().getClass(), checkedException.getClass());

    assertException(
        uncheckedException.getClass(),
        () ->
            CheckedExceptionUtils.wrap(
                () -> {
                  throw uncheckedException;
                }));

    CheckedExceptionUtils.wrap(
        () -> {
          // no return  , no exception throws
        });

    String value = CheckedExceptionUtils.wrap(() -> "test");

    assertEquals(value, "test");
  }

  @Test
  public void TestWrapRuntime() {
    Exception e =
        assertException(
            RuntimeException.class,
            () ->
                CheckedExceptionUtils.wrapRuntime(
                    () -> {
                      throw checkedException;
                    }));

    assertEquals(e.getCause().getClass(), checkedException.getClass());

    Exception e2 =
        assertException(
            RuntimeException.class,
            () ->
                CheckedExceptionUtils.wrapRuntime(
                    () -> {
                      throw uncheckedException;
                    }));

    assertEquals(e2.getCause().getClass(), uncheckedException.getClass());

    CheckedExceptionUtils.wrapRuntime(
        () -> {
          // no return  , no exception throws
        });

    String value = CheckedExceptionUtils.wrapRuntime(() -> "test");

    assertEquals(value, "test");
  }

  @Test
  public void TestWrapWtihHandler() {
    // this check exception is mapping to OharaTest0000Exception  in handler
    Exception e =
        assertException(
            OharaTest0000Exception.class,
            () ->
                CheckedExceptionUtils.wrap(
                    () -> {
                      throw checkedException;
                    },
                    handler));

    assertEquals(e.getCause().getClass(), checkedException.getClass());

    // user will catch like this
    try {
      CheckedExceptionUtils.wrap(
          () -> {
            throw checkedException;
          },
          handler);
    } catch (OharaTest0000Exception e2) {
      // catch here
    } catch (Exception e3) {
      fail("should be catch in OharaTest0000Exception");
    }

    assertException(
        uncheckedException.getClass(),
        () ->
            CheckedExceptionUtils.wrap(
                () -> {
                  throw uncheckedException;
                }));

    CheckedExceptionUtils.wrap(
        () -> {
          // no return  , no exception throws
        },
        handler);

    String value = CheckedExceptionUtils.wrap(() -> "test", handler);

    assertEquals(value, "test");
  }

  @Test
  public void TestWrapRuntimeWtihHandler() {
    // this check exception is mapping to OharaTest0000Exception  in handler
    Exception e =
        assertException(
            OharaTest0000Exception.class,
            () ->
                CheckedExceptionUtils.wrapRuntime(
                    () -> {
                      throw checkedException;
                    },
                    handler));
    assertEquals(e.getCause().getClass(), checkedException.getClass());

    // user will catch like this
    try {
      CheckedExceptionUtils.wrapRuntime(
          () -> {
            throw checkedException;
          },
          handler);
    } catch (OharaTest0000Exception e2) {
      // catch here
    } catch (Exception e3) {
      fail("should be catch in OharaTest0000Exception");
    }

    Exception e2 =
        assertException(
            OharaException.class,
            () ->
                CheckedExceptionUtils.wrapRuntime(
                    () -> {
                      throw uncheckedException;
                    },
                    handler));
    assertEquals(e2.getCause().getClass(), uncheckedException.getClass());

    CheckedExceptionUtils.wrapRuntime(
        () -> {
          // no return  , no exception throws
        },
        handler);

    String value = CheckedExceptionUtils.wrapRuntime(() -> "test", handler);

    assertEquals(value, "test");
  }

  @Test(expected = ClassNotFoundException.class)
  public void TestRethrowException() {
    // This method throw checked exception without check
    CheckedExceptionUtils.rethrow(
        () -> {
          throw new ClassNotFoundException();
        });
  }

  @Test(expected = ClassCastException.class)
  public void TestRethrowRuntimeException() {
    // This method throw checked exception without check
    CheckedExceptionUtils.rethrow(
        () -> {
          throw new ClassCastException();
        });
  }

  @Test
  public void TestRethrow() {
    // This method throw checked exception without check
    CheckedExceptionUtils.rethrow(
        () -> {
          // do nothing  , no exception throw
        });

    String value = CheckedExceptionUtils.rethrow(() -> "test");

    assertEquals(value, "test");
  }
}
