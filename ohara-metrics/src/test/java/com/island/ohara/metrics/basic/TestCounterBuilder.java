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

import com.island.ohara.common.rule.OharaTest;
import com.island.ohara.common.util.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

public class TestCounterBuilder extends OharaTest {

  @Test(expected = NullPointerException.class)
  public void testNullId() {
    Counter.builder().id(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyId() {
    Counter.builder().id("");
  }

  @Test(expected = NullPointerException.class)
  public void testNullGroup() {
    Counter.builder().group(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyGroup() {
    Counter.builder().group("");
  }

  @Test(expected = NullPointerException.class)
  public void testNullName() {
    Counter.builder().name(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyName() {
    Counter.builder().name("");
  }

  @Test(expected = NullPointerException.class)
  public void testNullUnit() {
    Counter.builder().unit(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyUnit() {
    Counter.builder().unit("");
  }

  @Test(expected = NullPointerException.class)
  public void testNullDocument() {
    Counter.builder().document(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyDocument() {
    Counter.builder().document("");
  }

  @Test
  public void testSetOnlyName() {
    try (Counter counter = Counter.builder().name(CommonUtils.randomString()).build()) {
      CommonUtils.requireNonEmpty(counter.getDocument());
      CommonUtils.requireNonEmpty(counter.getUnit());
      Assert.assertEquals(counter.name(), counter.group());
    }
  }

  @Test
  public void testSetters() {
    String group = CommonUtils.randomString();
    String name = CommonUtils.randomString();
    String document = CommonUtils.randomString();
    String unit = CommonUtils.randomString();
    long value = CommonUtils.current();
    long startTime = CommonUtils.current();
    try (Counter counter =
        Counter.builder()
            .group(group)
            .name(name)
            .value(value)
            .startTime(startTime)
            .document(document)
            .unit(unit)
            .build()) {
      Assert.assertEquals(group, counter.group());
      Assert.assertEquals(name, counter.name());
      Assert.assertEquals(document, counter.getDocument());
      Assert.assertEquals(value, counter.getValue());
      Assert.assertEquals(startTime, counter.getStartTime());
      Assert.assertEquals(unit, counter.getUnit());
    }
  }
}
