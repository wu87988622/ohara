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

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestCounterMBean extends SmallTest {

  @Test (expected = NullPointerException.class)
  public void testRegisterNullName() {
    CounterMBean.register(null);
  }

  @Test (expected = IllegalArgumentException.class)
  public void testRegisterEmptyName() {
    CounterMBean.register("");
  }

  @Test
  public void testFromBean() {
    Counter counter = CounterMBean.register(CommonUtils.randomString());
    List<CounterMBean> beans = BeanChannel.local().counterMBeans();
    Assert.assertEquals(1, beans.size());
    Assert.assertEquals(counter.name(), beans.get(0).name());

    counter.setAndGet(CommonUtils.current());

    CommonUtils.await(() -> BeanChannel.local().counterMBeans().get(0).getValue() == counter.getValue(),
      java.time.Duration.ofSeconds(10));
  }
}
