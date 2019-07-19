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

package com.island.ohara.metrics.kafka;

import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.CommonUtils;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

public class TestTopicMeter extends SmallTest {

  @Test(expected = NullPointerException.class)
  public void nullTopicName() {
    new TopicMeter(
        null,
        TopicMeter.Catalog.BytesInPerSec,
        CommonUtils.current(),
        CommonUtils.randomString(),
        (double) CommonUtils.current(),
        (double) CommonUtils.current(),
        (double) CommonUtils.current(),
        (double) CommonUtils.current(),
        TimeUnit.DAYS,
        CommonUtils.current());
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyTopicName() {
    new TopicMeter(
        "",
        TopicMeter.Catalog.BytesInPerSec,
        CommonUtils.current(),
        CommonUtils.randomString(),
        (double) CommonUtils.current(),
        (double) CommonUtils.current(),
        (double) CommonUtils.current(),
        (double) CommonUtils.current(),
        TimeUnit.DAYS,
        CommonUtils.current());
  }

  @Test(expected = NullPointerException.class)
  public void nullName() {
    new TopicMeter(
        CommonUtils.randomString(),
        null,
        CommonUtils.current(),
        CommonUtils.randomString(),
        (double) CommonUtils.current(),
        (double) CommonUtils.current(),
        (double) CommonUtils.current(),
        (double) CommonUtils.current(),
        TimeUnit.DAYS,
        CommonUtils.current());
  }

  @Test(expected = NullPointerException.class)
  public void nullEventType() {
    new TopicMeter(
        CommonUtils.randomString(),
        TopicMeter.Catalog.BytesInPerSec,
        CommonUtils.current(),
        null,
        (double) CommonUtils.current(),
        (double) CommonUtils.current(),
        (double) CommonUtils.current(),
        (double) CommonUtils.current(),
        TimeUnit.DAYS,
        CommonUtils.current());
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyEventType() {
    new TopicMeter(
        CommonUtils.randomString(),
        TopicMeter.Catalog.BytesInPerSec,
        CommonUtils.current(),
        "",
        (double) CommonUtils.current(),
        (double) CommonUtils.current(),
        (double) CommonUtils.current(),
        (double) CommonUtils.current(),
        TimeUnit.DAYS,
        CommonUtils.current());
  }

  @Test(expected = NullPointerException.class)
  public void nullTimeUnit() {
    new TopicMeter(
        CommonUtils.randomString(),
        TopicMeter.Catalog.BytesInPerSec,
        CommonUtils.current(),
        CommonUtils.randomString(),
        (double) CommonUtils.current(),
        (double) CommonUtils.current(),
        (double) CommonUtils.current(),
        (double) CommonUtils.current(),
        null,
        CommonUtils.current());
  }

  @Test
  public void testGetter() {
    String topicName = CommonUtils.randomString();
    TopicMeter.Catalog catalog = TopicMeter.Catalog.BytesInPerSec;
    long count = CommonUtils.current();
    String eventType = CommonUtils.randomString();
    double fifteenMinuteRate = (double) CommonUtils.current();
    double fiveMinuteRate = (double) CommonUtils.current();
    double meanRate = (double) CommonUtils.current();
    double oneMinuteRate = (double) CommonUtils.current();
    TimeUnit rateUnit = TimeUnit.HOURS;
    TopicMeter meter =
        new TopicMeter(
            topicName,
            catalog,
            count,
            eventType,
            fifteenMinuteRate,
            fiveMinuteRate,
            meanRate,
            oneMinuteRate,
            rateUnit,
            CommonUtils.current());

    Assert.assertEquals(topicName, meter.topicName());
    Assert.assertEquals(catalog, meter.catalog());
    Assert.assertEquals(count, meter.count());
    Assert.assertEquals(eventType, meter.eventType());
    Assert.assertEquals(fifteenMinuteRate, meter.fifteenMinuteRate(), 0);
    Assert.assertEquals(fiveMinuteRate, meter.fiveMinuteRate(), 0);
    Assert.assertEquals(meanRate, meter.meanRate(), 0);
    Assert.assertEquals(oneMinuteRate, meter.oneMinuteRate(), 0);
    Assert.assertEquals(rateUnit, meter.rateUnit());
  }
}
