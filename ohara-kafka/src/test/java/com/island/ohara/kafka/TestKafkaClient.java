package com.island.ohara.kafka;

import com.island.ohara.common.util.ReleaseOnce;
import com.island.ohara.integration.OharaTestUtil;
import com.island.ohara.integration.With3Brokers;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.After;
import org.junit.Test;

public class TestKafkaClient extends With3Brokers {
  private Duration timeout = Duration.ofSeconds(10);
  private OharaTestUtil testUtil = testUtil();

  private KafkaClient client = KafkaClient.of(testUtil.brokersConnProps());

  @Test
  public void testAddPartitions() {
    String topicName = methodName();
    client.topicCreator().numberOfPartitions(1).numberOfReplications((short) 1).create(topicName);
    assertEquals(client.topicDescription(topicName).numberOfPartitions(), 1);

    client.addPartitions(topicName, 2);
    assertEquals(client.topicDescription(topicName).numberOfPartitions(), 2);
    // decrease the number
    assertException(IllegalArgumentException.class, () -> client.addPartitions(topicName, 1));
    // alter an nonexistent topic
    assertException(IllegalArgumentException.class, () -> client.addPartitions("Xxx", 2, timeout));
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

  @After
  public void cleanup() {
    ReleaseOnce.close(client);
  }
}
