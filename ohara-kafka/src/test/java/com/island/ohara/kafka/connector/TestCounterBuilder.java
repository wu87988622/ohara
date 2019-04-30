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
import com.island.ohara.metrics.basic.Counter;
import org.junit.Assert;
import org.junit.Test;

public class TestCounterBuilder extends SmallTest {

  @Test(expected = NullPointerException.class)
  public void testNullGroup() {
    CounterBuilder.of().group(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyGroup() {
    CounterBuilder.of().group("");
  }

  @Test(expected = NullPointerException.class)
  public void testNullName() {
    CounterBuilder.of().name(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyName() {
    CounterBuilder.of().name("");
  }

  @Test(expected = NullPointerException.class)
  public void testNullUnit() {
    CounterBuilder.of().unit(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyUnit() {
    CounterBuilder.of().unit("");
  }

  @Test(expected = NullPointerException.class)
  public void testNullDocument() {
    CounterBuilder.of().document(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyDocument() {
    CounterBuilder.of().document("");
  }

  @Test
  public void testSimpleBuild() {
    String group = CommonUtils.randomString();
    String name = CommonUtils.randomString();
    String unit = CommonUtils.randomString();
    String document = CommonUtils.randomString();
    Counter counter =
        CounterBuilder.of().group(group).name(name).unit(unit).document(document).build();
    Assert.assertEquals(group, counter.group());
    Assert.assertEquals(name, counter.name());
    Assert.assertEquals(unit, counter.getUnit());
    Assert.assertEquals(document, counter.getDocument());
  }
}
