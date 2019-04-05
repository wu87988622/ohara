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

import javax.activation.CommandMap;

public class TestCounter extends SmallTest {

  @Test
  public void testIncrementAndGet() {
    Counter counter = Counter.builder()
      .value(0)
      .name(CommonUtils.randomString())
      .build();
    Assert.assertEquals(1, counter.incrementAndGet());
  }

  @Test
  public void testGetAndIncrement() {
    Counter counter = Counter.builder()
      .value(0)
      .name(CommonUtils.randomString())
      .build();
    Assert.assertEquals(0, counter.getAndIncrement());
  }

  @Test
  public void testDecrementAndGet() {
    Counter counter = Counter.builder()
      .value(0)
      .name(CommonUtils.randomString())
      .build();
    Assert.assertEquals(-1, counter.decrementAndGet());
  }

  @Test
  public void testGetAndDecrement() {
    Counter counter = Counter.builder()
      .value(0)
      .name(CommonUtils.randomString())
      .build();
    Assert.assertEquals(0, counter.getAndDecrement());
  }

  @Test
  public void testGetAndSet() {
    Counter counter = Counter.builder()
      .value(0)
      .name(CommonUtils.randomString())
      .build();
    Assert.assertEquals(0, counter.getAndSet(10));
  }

  @Test
  public void testSetAndGet() {
    Counter counter = Counter.builder()
      .value(0)
      .name(CommonUtils.randomString())
      .build();
    Assert.assertEquals(10, counter.setAndGet(10));
  }

  @Test
  public void testGetAndAdd() {
    Counter counter = Counter.builder()
      .value(0)
      .name(CommonUtils.randomString())
      .build();
    Assert.assertEquals(0, counter.getAndAdd(10));
  }

  @Test
  public void testAddAndGet() {
    Counter counter = Counter.builder()
      .value(0)
      .name(CommonUtils.randomString())
      .build();
    Assert.assertEquals(10, counter.addAndGet(10));
  }

  @Test
  public void testEquals() {
    String name = CommonUtils.randomString();
    Counter.Builder builder = Counter.builder()
      .value(CommonUtils.current())
      .startTime(CommonUtils.current())
      .name(CommonUtils.randomString());
    Assert.assertEquals(builder.build(), builder.build());
  }

  @Test
  public void testEqualsOnDiffDocument() {
    String name = CommonUtils.randomString();
    Counter.Builder builder = Counter.builder()
      .value(CommonUtils.current())
      .startTime(CommonUtils.current())
      .name(CommonUtils.randomString());
    Assert.assertEquals(builder.build(), builder.document(CommonUtils.randomString()).build());
  }

  @Test
  public void testHashCodesOnDiffDocument() {
    String name = CommonUtils.randomString();
    Counter.Builder builder = Counter.builder()
      .value(CommonUtils.current())
      .startTime(CommonUtils.current())
      .name(CommonUtils.randomString());
    Assert.assertEquals(builder.build().hashCode(), builder.document(CommonUtils.randomString()).build().hashCode());
  }

  @Test
  public void testHashCode() {
    String name = CommonUtils.randomString();
    Counter.Builder builder = Counter.builder()
      .value(CommonUtils.current())
      .startTime(CommonUtils.current())
      .name(CommonUtils.randomString());
    Assert.assertEquals(builder.build().hashCode(), builder.build().hashCode());
  }

  @Test
  public void testNotEqualsOnName() {
    String name = CommonUtils.randomString();
    Counter.Builder builder = Counter.builder()
      .value(CommonUtils.current())
      .startTime(CommonUtils.current())
      .name(CommonUtils.randomString());
    Assert.assertNotEquals(builder.build(), builder.name(CommonUtils.randomString()).build());
  }

  @Test
  public void testDiffHashCodeOnName() {
    String name = CommonUtils.randomString();
    Counter.Builder builder = Counter.builder()
      .value(CommonUtils.current())
      .startTime(CommonUtils.current())
      .name(CommonUtils.randomString());
    Assert.assertNotEquals(builder.build().hashCode(), builder.name(CommonUtils.randomString()).build().hashCode());
  }

  @Test
  public void testNotEqualsOnStartTime() {
    String name = CommonUtils.randomString();
    Counter.Builder builder = Counter.builder()
      .value(CommonUtils.current())
      .startTime(CommonUtils.current())
      .name(CommonUtils.randomString());
    Assert.assertNotEquals(builder.build(), builder.startTime(CommonUtils.current() + 1).build());
  }

  @Test
  public void testDiffHashCodeOnStartTime() {
    String name = CommonUtils.randomString();
    Counter.Builder builder = Counter.builder()
      .value(CommonUtils.current())
      .startTime(CommonUtils.current())
      .name(CommonUtils.randomString());
    Assert.assertNotEquals(builder.build().hashCode(), builder.startTime(CommonUtils.current() + 1).build().hashCode());
  }

  @Test
  public void testNotEqualsOnValue() {
    String name = CommonUtils.randomString();
    Counter.Builder builder = Counter.builder()
      .value(CommonUtils.current())
      .startTime(CommonUtils.current())
      .name(CommonUtils.randomString());
    Assert.assertNotEquals(builder.build(), builder.value(CommonUtils.current() + 1).build());
  }

  @Test
  public void testDiffHashCodeOnValue() {
    String name = CommonUtils.randomString();
    Counter.Builder builder = Counter.builder()
      .value(CommonUtils.current())
      .startTime(CommonUtils.current())
      .name(CommonUtils.randomString());
    Assert.assertNotEquals(builder.build().hashCode(), builder.value(CommonUtils.current() + 1).build().hashCode());
  }
}
