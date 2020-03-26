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

package oharastream.ohara.metrics.basic;

import oharastream.ohara.common.rule.OharaTest;
import oharastream.ohara.common.setting.ObjectKey;
import oharastream.ohara.common.util.CommonUtils;
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
    Counter.builder().key(null);
  }

  @Test(expected = NullPointerException.class)
  public void testNullName() {
    Counter.builder().item(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyName() {
    Counter.builder().item("");
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
  public void testSetters() {
    ObjectKey key = CommonUtils.randomKey();
    String name = CommonUtils.randomString();
    String document = CommonUtils.randomString();
    String unit = CommonUtils.randomString();
    long value = CommonUtils.current();
    long startTime = CommonUtils.current();
    try (Counter counter =
        Counter.builder()
            .key(key)
            .item(name)
            .value(value)
            .startTime(startTime)
            .document(document)
            .unit(unit)
            .build()) {
      Assert.assertEquals(key, counter.key());
      Assert.assertEquals(name, counter.item());
      Assert.assertEquals(document, counter.getDocument());
      Assert.assertEquals(value, counter.getValue());
      Assert.assertEquals(startTime, counter.getStartTime());
      Assert.assertEquals(unit, counter.getUnit());
    }
  }
}
