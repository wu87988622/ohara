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
import com.island.ohara.metrics.BeanChannel;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class TestCounter extends OharaTest {

  @Test
  public void testIncrementAndGet() {
    try (Counter counter = Counter.builder().value(0).name(CommonUtils.randomString(10)).build()) {
      Assert.assertEquals(1, counter.incrementAndGet());
    }
  }

  @Test
  public void testGetAndIncrement() {
    try (Counter counter = Counter.builder().value(0).name(CommonUtils.randomString(10)).build()) {
      Assert.assertEquals(0, counter.getAndIncrement());
    }
  }

  @Test
  public void testDecrementAndGet() {
    Counter counter = Counter.builder().value(0).name(CommonUtils.randomString(10)).build();
    Assert.assertEquals(-1, counter.decrementAndGet());
  }

  @Test
  public void testGetAndDecrement() {
    try (Counter counter = Counter.builder().value(0).name(CommonUtils.randomString(10)).build()) {
      Assert.assertEquals(0, counter.getAndDecrement());
    }
  }

  @Test
  public void testGetAndSet() {
    try (Counter counter = Counter.builder().value(0).name(CommonUtils.randomString(10)).build()) {
      Assert.assertEquals(0, counter.getAndSet(10));
    }
  }

  @Test
  public void testSetAndGet() {
    try (Counter counter = Counter.builder().value(0).name(CommonUtils.randomString(10)).build()) {
      Assert.assertEquals(10, counter.setAndGet(10));
    }
  }

  @Test
  public void testGetAndAdd() {
    try (Counter counter = Counter.builder().value(0).name(CommonUtils.randomString(10)).build()) {
      Assert.assertEquals(0, counter.getAndAdd(10));
    }
  }

  @Test
  public void testAddAndGet() {
    try (Counter counter = Counter.builder().value(0).name(CommonUtils.randomString(10)).build()) {
      Assert.assertEquals(10, counter.addAndGet(10));
    }
  }

  @Test
  public void testEquals() {
    try (Counter counter =
        Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString(10))
            .build()) {
      Assert.assertEquals(counter, counter);
    }
  }

  @Test
  public void testNotEqualsOnDiffGroup() {
    Counter.Builder builder =
        Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString(10))
            .group(CommonUtils.randomString());
    try (Counter c0 = builder.build();
        Counter c1 = builder.group(CommonUtils.randomString()).build()) {
      Assert.assertNotEquals(c0, c1);
    }
  }

  @Test
  public void testHashCodesOnDiffGroup() {
    Counter.Builder builder =
        Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString(10))
            .group(CommonUtils.randomString());
    try (Counter c0 = builder.build();
        Counter c1 = builder.group(CommonUtils.randomString()).build()) {
      Assert.assertNotEquals(c0.hashCode(), c1.hashCode());
    }
  }

  @Test
  public void testEqualsOnDiffDocument() {
    Counter.Builder builder =
        Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString(10));
    try (Counter c0 = builder.build();
        Counter c1 = builder.document(CommonUtils.randomString()).build()) {
      Assert.assertEquals(c0, c1);
    }
  }

  @Test
  public void testHashCodesOnDiffDocument() {
    Counter.Builder builder =
        Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString(10));
    try (Counter c0 = builder.build();
        Counter c1 = builder.document(CommonUtils.randomString()).build()) {
      Assert.assertEquals(c0.hashCode(), c1.hashCode());
    }
  }

  @Test
  public void testNotEqualsOnDiffUnit() {
    Counter.Builder builder =
        Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString(10));
    try (Counter c0 = builder.build();
        Counter c1 = builder.unit(CommonUtils.randomString()).build()) {
      Assert.assertNotEquals(c0, c1);
    }
  }

  @Test
  public void testHashCodesOnDiffUnit() {
    Counter.Builder builder =
        Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString(10));
    try (Counter c0 = builder.build();
        Counter c1 = builder.unit(CommonUtils.randomString()).build()) {
      Assert.assertNotEquals(c0.hashCode(), c1.hashCode());
    }
  }

  @Test
  public void testNotEqualsOnDiffValue() {
    Counter.Builder builder =
        Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString(10));
    try (Counter c0 = builder.build();
        Counter c1 = builder.value(CommonUtils.current() + 1000).build()) {
      Assert.assertNotEquals(c0, c1);
    }
  }

  @Test
  public void testHashCodesOnDiffValue() {
    Counter.Builder builder =
        Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString(10));
    try (Counter c0 = builder.build();
        Counter c1 = builder.value(CommonUtils.current() + 1000).build()) {
      Assert.assertNotEquals(c0.hashCode(), c1.hashCode());
    }
  }

  @Test
  public void testHashCode() {
    try (Counter counter =
        Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString(10))
            .build()) {
      Assert.assertEquals(counter.hashCode(), counter.hashCode());
    }
  }

  @Test
  public void testNotEqualsOnName() {
    Counter.Builder builder =
        Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString(10));
    try (Counter c0 = builder.build();
        Counter c1 = builder.name(CommonUtils.randomString(10)).build()) {
      Assert.assertNotEquals(c0, c1);
    }
  }

  @Test
  public void testDiffHashCodeOnName() {
    Counter.Builder builder =
        Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString(10));
    try (Counter c0 = builder.build();
        Counter c1 = builder.name(CommonUtils.randomString(10)).build()) {
      Assert.assertNotEquals(c0.hashCode(), c1.hashCode());
    }
  }

  /**
   * We generate a random id for each counter so it is ok to produce multiple counter in same jvm
   */
  @SuppressWarnings("try")
  @Test
  public void testDuplicateRegisterWithDiffId() {
    Counter.Builder builder =
        Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString(10));
    try (Counter c = builder.register();
        Counter c2 = builder.register()) {
      // good
    }
  }

  @SuppressWarnings("try")
  @Test(expected = IllegalArgumentException.class)
  public void testDuplicateRegister() {
    Counter.Builder builder =
        Counter.builder()
            .value(CommonUtils.current())
            .startTime(CommonUtils.current())
            .name(CommonUtils.randomString(10))
            .id(CommonUtils.randomString());
    try (Counter c = builder.register();
        Counter c2 = builder.register()) {
      throw new AssertionError();
    }
  }

  @Test
  public void testFromBean() {
    String group = CommonUtils.randomString();
    String name = CommonUtils.randomString();
    String document = CommonUtils.randomString();
    String unit = CommonUtils.randomString();
    try (Counter counter =
        Counter.builder().group(group).name(name).document(document).unit(unit).register()) {
      List<CounterMBean> beans = BeanChannel.local().counterMBeans();
      Assert.assertNotEquals(0, beans.size());
      CounterMBean bean = beans.stream().filter(c -> c.name().equals(name)).findFirst().get();
      Assert.assertEquals(counter.group(), bean.group());
      Assert.assertEquals(counter.name(), bean.name());
      Assert.assertEquals(counter.getDocument(), bean.getDocument());
      Assert.assertEquals(counter.getUnit(), bean.getUnit());
      Assert.assertEquals(group, bean.group());
      Assert.assertEquals(name, bean.name());
      Assert.assertEquals(document, bean.getDocument());
      Assert.assertEquals(unit, bean.getUnit());

      counter.setAndGet(CommonUtils.current());

      CommonUtils.await(
          () ->
              BeanChannel.local().counterMBeans().stream()
                      .filter(c -> c.name().equals(name))
                      .findFirst()
                      .get()
                      .getValue()
                  == counter.getValue(),
          java.time.Duration.ofSeconds(10));
    }
  }

  @Test
  public void testProperties() {
    try (Counter counter = Counter.builder().name(CommonUtils.randomString(10)).register()) {
      Assert.assertFalse(counter.properties.isEmpty());
      Assert.assertTrue(counter.needClose);
    }
    try (Counter counter = Counter.builder().name(CommonUtils.randomString(10)).build()) {
      Assert.assertFalse(counter.properties.isEmpty());
      Assert.assertFalse(counter.needClose);
    }
  }

  @Test
  public void testClose() {
    Map<String, String> properties;
    try (Counter counter = Counter.builder().name(CommonUtils.randomString(10)).register()) {
      Assert.assertEquals(
          1,
          BeanChannel.builder()
              .local()
              .domainName(Counter.DOMAIN)
              .properties(counter.properties)
              .local()
              .build()
              .size());
      properties = counter.properties;
    }
    Assert.assertEquals(
        0,
        BeanChannel.builder()
            .local()
            .domainName(Counter.DOMAIN)
            .properties(properties)
            .local()
            .build()
            .size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testZeroStartTime() {
    Counter.builder()
        .group("group")
        .name("name")
        .unit("unit")
        .document("document")
        .startTime(0)
        .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeStartTime() {
    Counter.builder()
        .group("group")
        .name("name")
        .unit("unit")
        .document("document")
        .startTime(-999)
        .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testZeroQueryTime() {
    Counter.builder()
        .group("group")
        .name("name")
        .unit("unit")
        .document("document")
        .queryTime(0)
        .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeQueryTime() {
    Counter.builder()
        .group("group")
        .name("name")
        .unit("unit")
        .document("document")
        .queryTime(-999)
        .build();
  }
}
