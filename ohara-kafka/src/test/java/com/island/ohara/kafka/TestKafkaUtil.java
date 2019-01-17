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

package com.island.ohara.kafka;

import com.island.ohara.integration.OharaTestUtil;
import com.island.ohara.integration.With3Brokers;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.Test;

public class TestKafkaUtil extends With3Brokers {
  private final OharaTestUtil testUtil = testUtil();

  @Test
  public void testAddPartitions() {
    String topicName = methodName();
    KafkaUtil.createTopic(testUtil.brokersConnProps(), topicName, 1, (short) 1);

    assertEquals(
        KafkaUtil.topicDescription(testUtil.brokersConnProps(), topicName).numberOfPartitions(), 1);

    KafkaUtil.addPartitions(testUtil.brokersConnProps(), topicName, 2);

    assertEquals(
        KafkaUtil.topicDescription(testUtil.brokersConnProps(), topicName).numberOfPartitions(), 2);
    // decrease the number

    assertException(
        IllegalArgumentException.class,
        () -> KafkaUtil.addPartitions(testUtil.brokersConnProps(), topicName, 1));
    // alter an nonexistent topic
    assertException(
        IllegalArgumentException.class,
        () -> KafkaUtil.addPartitions(testUtil.brokersConnProps(), "Xxx", 2));
  }

  @Test
  public void testCreate() {
    String topicName = methodName();
    int numberOfPartitions = 2;
    short numberOfReplications = (short) 2;
    KafkaUtil.createTopic(
        testUtil.brokersConnProps(), topicName, numberOfPartitions, numberOfReplications);

    TopicDescription topicInfo = KafkaUtil.topicDescription(testUtil.brokersConnProps(), topicName);
    assertEquals(topicInfo.name(), topicName);
    assertEquals(topicInfo.numberOfPartitions(), numberOfPartitions);
    assertEquals(topicInfo.numberOfReplications(), numberOfReplications);

    KafkaUtil.deleteTopic(testUtil.brokersConnProps(), topicName);
    assertFalse(KafkaUtil.exist(testUtil.brokersConnProps(), topicName));
  }

  @Test
  public void testTopicOptions() {
    String topicName = methodName();
    int numberOfPartitions = 2;
    short numberOfReplications = (short) 2;
    Map<String, String> options =
        Collections.singletonMap(
            TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE);

    KafkaUtil.createTopic(
        testUtil.brokersConnProps(), topicName, numberOfPartitions, numberOfReplications, options);

    TopicDescription topicInfo = KafkaUtil.topicDescription(testUtil.brokersConnProps(), topicName);

    assertEquals(topicInfo.name(), topicName);
    assertEquals(topicInfo.numberOfPartitions(), numberOfPartitions);
    assertEquals(topicInfo.numberOfReplications(), numberOfReplications);
    assertEquals(
        topicInfo
            .options()
            .stream()
            .filter(x -> Objects.equals(x.key(), TopicConfig.CLEANUP_POLICY_CONFIG))
            .collect(Collectors.toList())
            .get(0)
            .value(),
        TopicConfig.CLEANUP_POLICY_DELETE);
  }
}
