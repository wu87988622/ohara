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
import static com.island.ohara.streams.DataImporter.createKafkaProducer;

import com.island.ohara.integration.With3Brokers;
import com.island.ohara.kafka.BrokerClient;
import com.island.ohara.streams.ostream.Serdes;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"rawtypes", "unchecked"})
public class TestOStream extends With3Brokers {
  public static final Logger LOG = LoggerFactory.getLogger(TestOStream.class);
  private final String appid = "test-app";
  private final String fromTopic = "stream-in";
  private final String toTopic = "stream-out";

  private final BrokerClient client = BrokerClient.of(testUtil().brokersConnProps());
  private final String simple_string = "this is a test sentence.";
  private final KafkaProducer<String, String> producer =
      createKafkaProducer(client.connectionProps());
  private final KafkaConsumer<String, String> consumer =
      createKafkaConsumer(client.connectionProps());

  @Before
  public void tearUp() {
    int partitions = 3;
    short replications = 1;
    try {
      client
          .topicCreator()
          .numberOfPartitions(partitions)
          .numberOfReplications(replications)
          .create(fromTopic);
      client
          .topicCreator()
          .numberOfPartitions(partitions)
          .numberOfReplications(replications)
          .create(toTopic);
    } catch (Exception e) {
      LOG.error(e.getMessage());
    }
  }

  @Test
  public void testSimpleApplication() {
    // Sending data
    ProducerRecord<String, String> producerRecord = new ProducerRecord<>(fromTopic, simple_string);
    Future<RecordMetadata> f = producer.send(producerRecord);
    try {
      f.get(10, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      e.printStackTrace();
    }

    // Checking data exists
    consumer.subscribe(Collections.singletonList(fromTopic));
    ConsumerRecords<String, String> m = consumer.poll(10000);
    consumer.commitAsync();
    Assert.assertEquals(1, m.count());
    m.forEach(
        record -> {
          Assert.assertEquals(simple_string, record.value());
        });

    // Ohara Streams ETL Part
    OStream<String, String> ostream =
        OStream.builder()
            .appid(appid)
            .bootstrapServers(client.connectionProps())
            .fromTopicWith(fromTopic, Serdes.STRING, Serdes.STRING)
            .toTopicWith(toTopic, Serdes.STRING, Serdes.STRING)
            .cleanStart()
            .build();

    ostream.filter(((key, value) -> value != null)).mapValues(String::toUpperCase).start();

    // Checking data again whether the ohara streams works or not
    consumer.unsubscribe();
    consumer.subscribe(Collections.singletonList(toTopic));
    try {
      ConsumerRecords<String, String> messages = consumer.poll(10000);
      consumer.commitAsync();
      Assert.assertEquals(1, messages.count());
      messages.forEach(record -> Assert.assertEquals(simple_string.toUpperCase(), record.value()));
    } finally {
      consumer.close();
    }
  }

  @After
  public void tearDown() {
    client.close();
    producer.close();
  }
}
