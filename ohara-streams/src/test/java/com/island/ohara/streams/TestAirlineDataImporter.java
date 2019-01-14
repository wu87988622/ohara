package com.island.ohara.streams;

import static com.island.ohara.streams.AirlineDataImporter.createKafkaConsumer;

import com.island.ohara.integration.With3Brokers;
import com.island.ohara.kafka.KafkaClient;
import com.island.ohara.kafka.TopicDescription;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.Assert;
import org.junit.Test;

public class TestAirlineDataImporter extends With3Brokers {

  private KafkaClient client = KafkaClient.of(testUtil().brokersConnProps());
  private List<String> TOPICS = Arrays.asList("carriers", "plane", "airport", "flight");

  @Test
  public void testCreateTopics() {
    int partitions = 1;
    short replications = 1;
    TOPICS.forEach(
        topic -> {
          client
              .topicCreator()
              .numberOfPartitions(partitions)
              .numberOfReplications(replications)
              .create(topic);
        });

    TOPICS.forEach(
        topic -> {
          TopicDescription desc = client.topicDescription(topic);
          Assert.assertEquals(topic, desc.name());
          Assert.assertEquals(partitions, desc.numberOfPartitions());
          Assert.assertEquals(replications, desc.numberOfReplications());
        });
  }

  @Test
  public void testImportAirlineData() {

    AirlineDataImporter.importData(client.brokers(), false);

    TOPICS.forEach(
        topic -> {
          Consumer<String, String> consumer = createKafkaConsumer(client.brokers());
          consumer.subscribe(Collections.singletonList(topic));

          try {
            ConsumerRecords<String, String> messages = consumer.poll(10000);
            consumer.commitAsync();
            Assert.assertTrue(messages.count() > 0);
          } finally {
            consumer.close();
          }
        });
  }
}
