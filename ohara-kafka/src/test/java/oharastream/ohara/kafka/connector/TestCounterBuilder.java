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

package oharastream.ohara.kafka.connector;

import oharastream.ohara.common.rule.OharaTest;
import oharastream.ohara.common.setting.ObjectKey;
import oharastream.ohara.common.util.CommonUtils;
import oharastream.ohara.metrics.basic.Counter;
import org.junit.Assert;
import org.junit.Test;

public class TestCounterBuilder extends OharaTest {

  @Test(expected = NullPointerException.class)
  public void testNullGroup() {
    CounterBuilder.of().key(null);
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
    ObjectKey key = ObjectKey.of(CommonUtils.randomString(), CommonUtils.randomString());
    String name = CommonUtils.randomString();
    String unit = CommonUtils.randomString();
    String document = CommonUtils.randomString();
    Counter counter = CounterBuilder.of().key(key).name(name).unit(unit).document(document).build();
    Assert.assertEquals(key, counter.key());
    Assert.assertEquals(name, counter.item());
    Assert.assertEquals(unit, counter.getUnit());
    Assert.assertEquals(document, counter.getDocument());
  }
}
