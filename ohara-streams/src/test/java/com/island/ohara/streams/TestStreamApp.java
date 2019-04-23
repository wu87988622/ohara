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

package com.island.ohara.streams;

import com.island.ohara.common.rule.SmallTest;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public class TestStreamApp extends SmallTest {

  @Test
  public void testCanFindCustomClassEntry() {
    CustomStreamApp app = new CustomStreamApp();
    StreamApp.runStreamApp(app.getClass());
  }

  public static class CustomStreamApp extends StreamApp {
    final AtomicInteger counter = new AtomicInteger();

    @Override
    public void init() {
      int res = counter.incrementAndGet();
      // StreamApp should call init() first
      Assert.assertEquals(1, res);
    }

    @Override
    public void start() {
      int res = counter.incrementAndGet();
      // StreamApp should call start() after init()
      Assert.assertEquals(2, res);
    }
  }
}
