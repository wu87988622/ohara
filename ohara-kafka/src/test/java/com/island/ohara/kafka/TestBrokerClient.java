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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.island.ohara.common.util.Releasable;
import com.island.ohara.kafka.exception.OharaExecutionException;
import com.island.ohara.testing.OharaTestUtils;
import com.island.ohara.testing.With3Brokers;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.TopicExistsException;
import org.junit.After;
import org.junit.Test;

public class TestBrokerClient extends With3Brokers {
  private final OharaTestUtils testUtil = testUtil();

  private final BrokerClient client = BrokerClient.of(testUtil.brokersConnProps());

  @Test
  public void testAddPartitions() {
    String topicName = methodName();
    client.topicCreator().numberOfPartitions(1).numberOfReplications((short) 1).create(topicName);
    assertEquals(client.topicDescription(topicName).numberOfPartitions(), 1);

    client.createPartitions(topicName, 2);
    assertEquals(client.topicDescription(topicName).numberOfPartitions(), 2);
    // decrease the number
    assertException(IllegalArgumentException.class, () -> client.createPartitions(topicName, 1));
    // alter an nonexistent topic
    assertException(IllegalArgumentException.class, () -> client.createPartitions("Xxx", 2));
  }

  @Test
  public void testCreate() {
    String topicName = methodName();
    int numberOfPartitions = 2;
    short numberOfReplications = (short) 2;
    client
        .topicCreator()
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .create(topicName);

    TopicDescription topicInfo = client.topicDescription(topicName);

    assertEquals(topicInfo.name(), topicName);
    assertEquals(topicInfo.numberOfPartitions(), numberOfPartitions);
    assertEquals(topicInfo.numberOfReplications(), numberOfReplications);

    assertEquals(
        client
            .topicDescriptions()
            .stream()
            .filter(t -> t.name().equals(topicName))
            .findFirst()
            .get(),
        topicInfo);

    client.deleteTopic(topicName);
    assertFalse(client.exist(topicName));
  }

  @Test
  public void testTopicOptions() {
    String topicName = methodName();
    int numberOfPartitions = 2;
    short numberOfReplications = (short) 2;
    Map<String, String> options =
        Collections.singletonMap(
            TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE);
    client
        .topicCreator()
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .options(options)
        .create(topicName);

    TopicDescription topicInfo = client.topicDescription(topicName);

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

  /** for OHARA-941 */
  @Test
  public void testExistTopic() {
    try {
      client
          .topicCreator()
          .numberOfPartitions(1)
          .numberOfReplications((short) 1)
          // enable kafka save the latest message for each key
          .deleted()
          .timeout(java.time.Duration.ofSeconds(30))
          .create(methodName());

      client
          .topicCreator()
          .numberOfPartitions(1)
          .numberOfReplications((short) 1)
          // enable kafka save the latest message for each key
          .deleted()
          .timeout(java.time.Duration.ofSeconds(30))
          .create(methodName());
    } catch (OharaExecutionException e) {
      assertTrue(e.getCause() instanceof TopicExistsException);
    }
  }

  @After
  public void cleanup() {
    Releasable.close(client);
  }
}
