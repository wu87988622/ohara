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

import com.island.ohara.common.data.Serializer;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.testing.WithBroker;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.CommonClientConfigs;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestProducerToConsumer extends WithBroker {

  private final String topicName = CommonUtils.randomString();

  @Before
  public void setup() {
    try (BrokerClient client = BrokerClient.of(testUtil().brokersConnProps())) {
      client
          .topicCreator()
          .numberOfPartitions(1)
          .numberOfReplications((short) 1)
          .topicName(topicName)
          .create();
    }
  }

  @Test
  public void testTimestamp() {
    long timestamp = CommonUtils.current();
    try (Producer<String, String> producer =
        Producer.<String, String>builder()
            .keySerializer(Serializer.STRING)
            .valueSerializer(Serializer.STRING)
            .connectionProps(testUtil().brokersConnProps())
            .build()) {
      producer.sender().key("a").value("b").topicName(topicName).timestamp(timestamp).send();
    }
    try (Consumer<String, String> consumer =
        Consumer.<String, String>builder()
            .keySerializer(Serializer.STRING)
            .valueSerializer(Serializer.STRING)
            .offsetFromBegin()
            .topicName(topicName)
            .connectionProps(testUtil().brokersConnProps())
            .build()) {
      List<Consumer.Record<String, String>> records = consumer.poll(Duration.ofSeconds(30), 1);
      Assert.assertEquals(1, records.size());
      Assert.assertEquals(timestamp, records.get(0).timestamp());
      Assert.assertEquals(TimestampType.CREATE_TIME, records.get(0).timestampType());
    }
  }

  @Test
  public void testResetConsumer() {
    try (Producer<String, String> producer =
        Producer.<String, String>builder()
            .keySerializer(Serializer.STRING)
            .valueSerializer(Serializer.STRING)
            .connectionProps(testUtil().brokersConnProps())
            .build()) {
      for (int i = 0; i < 100; i++)
        producer.sender().key("key" + i).value("value" + i).topicName(topicName).send();
    }

    try (Consumer<String, String> consumer =
        Consumer.<String, String>builder()
            .keySerializer(Serializer.STRING)
            .valueSerializer(Serializer.STRING)
            .offsetFromBegin()
            .topicName(topicName)
            .connectionProps(testUtil().brokersConnProps())
            .build()) {
      List<Consumer.Record<String, String>> record1s = consumer.poll(Duration.ofSeconds(30), 100);
      Assert.assertEquals(100, record1s.size());

      List<Consumer.Record<String, String>> record2s = consumer.poll(Duration.ofSeconds(1), 0);
      Assert.assertEquals(0, record2s.size());

      consumer.seekToBeginning(consumer.assignment()); // Reset topic to beginning

      List<Consumer.Record<String, String>> record3s = consumer.poll(Duration.ofSeconds(30), 100);
      Assert.assertEquals(100, record3s.size());
    }

    try (Producer<String, String> producer =
        Producer.<String, String>builder()
            .keySerializer(Serializer.STRING)
            .valueSerializer(Serializer.STRING)
            .connectionProps(testUtil().brokersConnProps())
            .build()) {
      for (int i = 0; i < 100; i++)
        producer.sender().key("key" + i).value("value" + i).topicName(topicName).send();
    }

    try (Consumer<String, String> consumer =
        Consumer.<String, String>builder()
            .keySerializer(Serializer.STRING)
            .valueSerializer(Serializer.STRING)
            .offsetFromBegin()
            .topicName(topicName)
            .connectionProps(testUtil().brokersConnProps())
            .build()) {
      List<Consumer.Record<String, String>> record1s = consumer.poll(Duration.ofSeconds(30), 200);
      Assert.assertEquals(200, record1s.size());

      consumer.seekToBeginning();
      List<Consumer.Record<String, String>> record2s = consumer.poll(Duration.ofSeconds(30), 200);
      Assert.assertEquals(200, record2s.size());

      List<Consumer.Record<String, String>> record3s = consumer.poll(Duration.ofSeconds(1), 0);
      Assert.assertEquals(0, record3s.size());
    }
  }

  @Test
  public void testOffset() {
    try (Producer<String, String> producer =
        Producer.<String, String>builder()
            .keySerializer(Serializer.STRING)
            .valueSerializer(Serializer.STRING)
            .connectionProps(testUtil().brokersConnProps())
            .build()) {
      for (int i = 0; i < 100; i++)
        producer.sender().key("key" + i).value("value" + i).topicName(topicName).send();
    }

    try (Consumer<String, String> consumer =
        Consumer.<String, String>builder()
            .keySerializer(Serializer.STRING)
            .valueSerializer(Serializer.STRING)
            .offsetFromBegin()
            .topicName(topicName)
            .connectionProps(testUtil().brokersConnProps())
            .build()) {
      List<Consumer.Record<String, String>> records = consumer.poll(Duration.ofSeconds(30), 1);
      Assert.assertEquals(100, records.size());

      Assert.assertEquals(0, records.get(0).offset());
      Assert.assertEquals("key0", records.get(0).key().get());
      Assert.assertEquals("value0", records.get(0).value().get());

      Assert.assertEquals(50, records.get(50).offset());
      Assert.assertEquals("key50", records.get(50).key().get());
      Assert.assertEquals("value50", records.get(50).value().get());

      Assert.assertEquals(99, records.get(99).offset());
      Assert.assertEquals("key99", records.get(99).key().get());
      Assert.assertEquals("value99", records.get(99).value().get());
    }
  }

  @Test
  public void normalCase() throws ExecutionException, InterruptedException {
    try (Producer<String, String> producer =
        Producer.<String, String>builder()
            .keySerializer(Serializer.STRING)
            .valueSerializer(Serializer.STRING)
            .connectionProps(testUtil().brokersConnProps())
            .build()) {
      Producer.RecordMetadata metadata =
          producer.sender().key("a").value("b").topicName(topicName).send().get();
      Assert.assertEquals(metadata.topicName(), topicName);
      try (Consumer<String, String> consumer =
          Consumer.<String, String>builder()
              .keySerializer(Serializer.STRING)
              .valueSerializer(Serializer.STRING)
              .offsetFromBegin()
              .topicName(topicName)
              .connectionProps(testUtil().brokersConnProps())
              .build()) {
        List<Consumer.Record<String, String>> records = consumer.poll(Duration.ofSeconds(30), 1);
        Assert.assertEquals(1, records.size());
        Assert.assertEquals("a", records.get(0).key().get());
        Assert.assertEquals("b", records.get(0).value().get());
        Assert.assertEquals(0, records.get(0).offset());
      }
    }
  }

  @Test
  public void withIdleTime() throws ExecutionException, InterruptedException {
    long timeout = 5000;
    try (Producer<String, String> producer =
        Producer.<String, String>builder()
            .keySerializer(Serializer.STRING)
            .valueSerializer(Serializer.STRING)
            .connectionProps(testUtil().brokersConnProps())
            .options(
                Collections.singletonMap(
                    CommonClientConfigs.CONNECTIONS_MAX_IDLE_MS_CONFIG, String.valueOf(timeout)))
            .build()) {
      Assert.assertEquals(
          producer.sender().key("a").value("b").topicName(topicName).send().get().topicName(),
          topicName);
      try (Consumer<String, String> consumer =
          Consumer.<String, String>builder()
              .keySerializer(Serializer.STRING)
              .valueSerializer(Serializer.STRING)
              .offsetFromBegin()
              .topicName(topicName)
              .connectionProps(testUtil().brokersConnProps())
              .options(
                  Collections.singletonMap(
                      CommonClientConfigs.CONNECTIONS_MAX_IDLE_MS_CONFIG, String.valueOf(timeout)))
              .build()) {
        List<Consumer.Record<String, String>> records = consumer.poll(Duration.ofSeconds(30), 1);
        Assert.assertEquals(1, records.size());
        Assert.assertEquals("a", records.get(0).key().get());
        Assert.assertEquals("b", records.get(0).value().get());

        TimeUnit.MILLISECONDS.sleep(timeout * 2);
        Assert.assertEquals(
            producer.sender().key("c").value("d").topicName(topicName).send().get().topicName(),
            topicName);
        List<Consumer.Record<String, String>> records2 = consumer.poll(Duration.ofSeconds(30), 1);
        Assert.assertEquals(1, records2.size());
        Assert.assertEquals("c", records2.get(0).key().get());
        Assert.assertEquals("d", records2.get(0).value().get());
      }
    }
  }

  @After
  public void tearDown() {
    try (BrokerClient client = BrokerClient.of(testUtil().brokersConnProps())) {
      client.deleteTopic(topicName);
    }
  }
}
