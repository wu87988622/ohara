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

import org.junit.Assume;

/** import or extends to simplify test */
public abstract class OharaTest {

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
      if (c.isInstance(e)) return c.cast(e);
      else
        throw new AssertionError(
            String.format(
                "Assert ERROR: The %s throws ,but the expected exception is  %s ",
                e.getClass().getName(), c.getName()),
            e);
    }
    throw new AssertionError("Assert ERROR: No exception throws");
  }

  /**
   * Skip all remaining test cases after calling this method.
   *
   * @param message why you want to skip all test cases?
   */
  @SuppressWarnings("ConstantConditions")
  protected void skipTest(String message) {
    Assume.assumeTrue(message, false);
  }
}
