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

package com.island.ohara.kafka.connector;

import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.CommonUtils;
import org.junit.Test;

public class TestCounterBuilder extends SmallTest {

  @Test(expected = NullPointerException.class)
  public void testNullGroup() {
    new CounterBuilder(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyGroup() {
    new CounterBuilder("");
  }

  @Test(expected = NullPointerException.class)
  public void testNullName() {
    new CounterBuilder(CommonUtils.randomString()).name(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyName() {
    new CounterBuilder(CommonUtils.randomString()).name("");
  }

  @Test(expected = NullPointerException.class)
  public void testNullUnit() {
    new CounterBuilder(CommonUtils.randomString()).unit(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyUnit() {
    new CounterBuilder(CommonUtils.randomString()).unit("");
  }

  @Test(expected = NullPointerException.class)
  public void testNullDocument() {
    new CounterBuilder(CommonUtils.randomString()).document(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyDocument() {
    new CounterBuilder(CommonUtils.randomString()).document("");
  }

  @Test
  public void testSimpleBuild() {
    new CounterBuilder(CommonUtils.randomString()).name(CommonUtils.randomString()).build();
  }
}
