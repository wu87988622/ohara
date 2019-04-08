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
import com.island.ohara.metrics.BeanChannel;
import org.junit.Assert;
import org.junit.Test;

import javax.activation.CommandMap;
import javax.management.InstanceAlreadyExistsException;
import java.util.List;

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
    Counter counter = Counter.builder()
      .value(CommonUtils.current())
      .startTime(CommonUtils.current())
      .name(CommonUtils.randomString())
            .build();
    Assert.assertEquals(counter, counter);
  }

  @Test
  public void testEqualsOnDiffDocument() {
    Counter.Builder builder = Counter.builder()
      .value(CommonUtils.current())
      .startTime(CommonUtils.current())
      .name(CommonUtils.randomString());
    Assert.assertEquals(builder.build(), builder.document(CommonUtils.randomString()).build());
  }

  @Test
  public void testHashCodesOnDiffDocument() {
    Counter.Builder builder = Counter.builder()
      .value(CommonUtils.current())
      .startTime(CommonUtils.current())
      .name(CommonUtils.randomString());
    Assert.assertEquals(builder.build().hashCode(), builder.document(CommonUtils.randomString()).build().hashCode());
  }

  @Test
  public void testNotEqualsOnDiffUnit() {
    Counter.Builder builder = Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString());
    Assert.assertNotEquals(builder.build(), builder.unit(CommonUtils.randomString()).build());
  }

  @Test
  public void testHashCodesOnDiffUnit() {
    Counter.Builder builder = Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString());
    Assert.assertNotEquals(builder.build().hashCode(), builder.unit(CommonUtils.randomString()).build().hashCode());
  }

  @Test
  public void testNotEqualsOnDiffValue() {
    Counter.Builder builder = Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString());
    Assert.assertNotEquals(builder.build(), builder.value(CommonUtils.current() + 1000).build());
  }

  @Test
  public void testHashCodesOnDiffValue() {
    Counter.Builder builder = Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString());
    Assert.assertNotEquals(builder.build().hashCode(), builder.value(CommonUtils.current() + 1000).build().hashCode());
  }

  @Test
  public void testHashCode() {
    Counter counter = Counter.builder()
      .value(CommonUtils.current())
      .startTime(CommonUtils.current())
      .name(CommonUtils.randomString())
      .build();
    Assert.assertEquals(counter.hashCode(), counter.hashCode());
  }

  @Test
  public void testNotEqualsOnName() {
    Counter.Builder builder = Counter.builder()
      .value(CommonUtils.current())
      .startTime(CommonUtils.current())
      .name(CommonUtils.randomString());
    Assert.assertNotEquals(builder.build(), builder.name(CommonUtils.randomString()).build());
  }

  @Test
  public void testDiffHashCodeOnName() {
    Counter.Builder builder = Counter.builder()
      .value(CommonUtils.current())
      .startTime(CommonUtils.current())
      .name(CommonUtils.randomString());
    Assert.assertNotEquals(builder.build().hashCode(), builder.name(CommonUtils.randomString()).build().hashCode());
  }

  @Test
  public void illegalInUsingSameName() {
    Counter.Builder builder = Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString());
    // pass
    builder.register();
    // fail
    try {
      builder.register();
      throw new AssertionError("this should not happen!!!");
    } catch (IllegalArgumentException e) {
      // pass
    }
  }

  @Test
  public void testFromBean() {
    String name = CommonUtils.randomString();
    String document = CommonUtils.randomString();
    String unit = CommonUtils.randomString();
    Counter counter = Counter.builder()
            .name(name)
            .document(document)
            .unit(unit)
            .register();
    List<CounterMBean> beans = BeanChannel.local().counterMBeans();
    Assert.assertNotEquals(0, beans.size());
    CounterMBean bean = beans.stream().filter(c -> c.name().equals(name)).findFirst().get();
    Assert.assertEquals(counter.name(), bean.name());
    Assert.assertEquals(counter.getDocument(), bean.getDocument());
    Assert.assertEquals(counter.getUnit(), bean.getUnit());
    Assert.assertEquals(name, bean.name());
    Assert.assertEquals(document, bean.getDocument());
    Assert.assertEquals(unit, bean.getUnit());

    counter.setAndGet(CommonUtils.current());

    CommonUtils.await(() -> BeanChannel.local().counterMBeans()
                    .stream()
                    .filter(c -> c.name().equals(name))
                    .findFirst()
                    .get()
                    .getValue() == counter.getValue(),
            java.time.Duration.ofSeconds(10));
  }
}
