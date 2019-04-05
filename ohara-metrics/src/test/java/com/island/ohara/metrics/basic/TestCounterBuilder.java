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

package com.island.ohara.metrics.basic;

import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

public class TestCounterBuilder extends SmallTest {

  @Test (expected = NullPointerException.class)
  public void testNullName() {
    Counter.builder().name(null);
  }

  @Test (expected = IllegalArgumentException.class)
  public void testEmptyName() {
    Counter.builder().name("");
  }

  @Test
  public void testSetOnlyName() {
    Counter.builder().name(CommonUtils.randomString()).build();
  }

  @Test
  public void testSetters() {
    String name = CommonUtils.randomString();
    String document = CommonUtils.randomString();
    long value = CommonUtils.current();
    long startTime = CommonUtils.current();
    Counter counter = Counter.builder()
      .name(name)
      .value(value)
      .startTime(startTime)
      .document(document)
      .build();
    Assert.assertEquals(name, counter.name());
    Assert.assertEquals(document, counter.getDocument());
    Assert.assertEquals(value, counter.getValue());
    Assert.assertEquals(startTime, counter.getStartTime());
  }

}
