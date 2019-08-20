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

package com.island.ohara.streams.ostream;

import com.island.ohara.common.data.Cell;
import com.island.ohara.common.data.Pair;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Serializer;
import com.island.ohara.common.setting.TopicKey;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.BrokerClient;
import com.island.ohara.kafka.Consumer;
import com.island.ohara.kafka.Producer;
import com.island.ohara.metrics.BeanChannel;
import com.island.ohara.streams.OStream;
import com.island.ohara.streams.StreamApp;
import com.island.ohara.streams.StreamTestUtils;
import com.island.ohara.streams.config.StreamDefUtils;
import com.island.ohara.streams.config.StreamDefinitions;
import com.island.ohara.streams.metric.MetricFactory;
import com.island.ohara.testing.WithBroker;
import java.time.Duration;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

// TODO: the streamapp requires many arguments from env variables.
// This tests do not care the rules required by streamapp.
// Fortunately (or unfortunately), streamapp lacks of enough checks to variables so the
// non-completed settings to streamapp works well in this test ... by chia
public class TestSimpleStreamCounter extends WithBroker {

  private static Duration timeout = Duration.ofSeconds(10);
  private static TopicKey fromKey = TopicKey.of(CommonUtils.randomString(), "metric-from");
  private static TopicKey toKey = TopicKey.of(CommonUtils.randomString(), "metric-to");

  private final BrokerClient client = BrokerClient.of(testUtil().brokersConnProps());
  private final Producer<Row, byte[]> producer =
      Producer.<Row, byte[]>builder()
          .connectionProps(client.connectionProps())
          .keySerializer(Serializer.ROW)
          .valueSerializer(Serializer.BYTES)
          .build();
  private final Consumer<Row, byte[]> consumer =
      Consumer.<Row, byte[]>builder()
          .topicName(toKey.topicNameOnKafka())
          .connectionProps(client.connectionProps())
          .keySerializer(Serializer.ROW)
          .valueSerializer(Serializer.BYTES)
          .build();

  @Before
  public void setup() {
    int partitions = 1;
    short replications = 1;
    client
        .topicCreator()
        .numberOfPartitions(partitions)
        .numberOfReplications(replications)
        .topicName(fromKey.topicNameOnKafka())
        .create();

    try {
      producer
          .sender()
          .key(Row.of(Cell.of("bar", "foo")))
          .value(new byte[0])
          .topicName(fromKey.topicNameOnKafka())
          .send()
          .get();
      producer
          .sender()
          .key(Row.of(Cell.of("hello", "world")))
          .value(new byte[0])
          .topicName(fromKey.topicNameOnKafka())
          .send()
          .get();
    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testMetrics() {
    // initial environment
    StreamTestUtils.setOharaEnv(
        Stream.of(
                Pair.of(StreamDefUtils.NAME_DEFINITION.key(), "metric-test"),
                Pair.of(StreamDefUtils.BROKER_DEFINITION.key(), client.connectionProps()),
                Pair.of(
                    StreamDefUtils.FROM_TOPIC_KEYS_DEFINITION.key(),
                    TopicKey.toJsonString(Collections.singletonList(fromKey))),
                Pair.of(
                    StreamDefUtils.TO_TOPIC_KEYS_DEFINITION.key(),
                    TopicKey.toJsonString(Collections.singletonList(toKey))))
            .collect(Collectors.toMap(Pair::left, Pair::right)));

    DirectWriteStreamApp app = new DirectWriteStreamApp();
    StreamApp.runStreamApp(app.getClass());

    // wait until topic has data
    CommonUtils.await(() -> consumer.poll(timeout).size() > 0, Duration.ofSeconds(30));

    // there should be two counter bean (in_topic, to_topic)
    Assert.assertEquals(2, BeanChannel.local().counterMBeans().size());

    BeanChannel.local()
        .counterMBeans()
        .forEach(
            bean -> {
              if (bean.name().equals(MetricFactory.IOType.TOPIC_IN.name()))
                // input counter bean should have exactly two record size
                Assert.assertEquals(2, Math.toIntExact(bean.getValue()));
              else
                // output counter bean should have exactly one record size (after filter)
                Assert.assertEquals(1, Math.toIntExact(bean.getValue()));
            });
  }

  public static class DirectWriteStreamApp extends StreamApp {

    @Override
    public void start(OStream<Row> ostream, StreamDefinitions streamDefinitions) {

      ostream.filter(row -> row.names().contains("bar")).map(row -> row).start();
    }
  }
}
