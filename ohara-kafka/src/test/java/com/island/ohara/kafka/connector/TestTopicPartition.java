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
import org.junit.Assert;
import org.junit.Test;

public class TestTopicPartition extends SmallTest {

  @Test(expected = NullPointerException.class)
  public void nullTopic() {
    new TopicPartition(null, 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyTopic() {
    new TopicPartition("", 1);
  }

  @Test
  public void testGetter() {
    String topicName = CommonUtils.randomString();
    int partition = (int) CommonUtils.current();
    TopicPartition topicPartition = new TopicPartition(topicName, partition);
    Assert.assertEquals(topicName, topicPartition.topicName());
    Assert.assertEquals(partition, topicPartition.partition());
  }

  @Test
  public void testEquals() {
    TopicPartition topicPartition =
        new TopicPartition(CommonUtils.randomString(), (int) CommonUtils.current());
    Assert.assertEquals(topicPartition, topicPartition);
    Assert.assertEquals(
        topicPartition, new TopicPartition(topicPartition.topicName(), topicPartition.partition()));
  }

  @Test
  public void testHashCode() {
    TopicPartition topicPartition =
        new TopicPartition(CommonUtils.randomString(), (int) CommonUtils.current());
    Assert.assertEquals(topicPartition.hashCode(), topicPartition.hashCode());
    Assert.assertEquals(
        topicPartition.hashCode(),
        new TopicPartition(topicPartition.topicName(), topicPartition.partition()).hashCode());
  }
}
