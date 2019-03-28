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

package com.island.ohara.streams;

import static com.island.ohara.streams.DataImporter.createKafkaConsumer;

import com.island.ohara.kafka.BrokerClient;
import com.island.ohara.kafka.TopicDescription;
import com.island.ohara.testing.With3Brokers;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.Assert;
import org.junit.Test;

public class TestDataImporter extends With3Brokers {

  private BrokerClient client = BrokerClient.of(testUtil().brokersConnProps());
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
              .topicName(topic)
              .create();
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

    DataImporter.importData(client.connectionProps(), false);

    TOPICS.forEach(
        topic -> {
          Consumer<String, String> consumer = createKafkaConsumer(client.connectionProps());
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
