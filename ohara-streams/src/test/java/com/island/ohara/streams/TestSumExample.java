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

import com.island.ohara.common.data.Cell;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Serializer;
import com.island.ohara.kafka.BrokerClient;
import com.island.ohara.kafka.Producer;
import com.island.ohara.streams.config.StreamDefinitions;
import com.island.ohara.streams.examples.SumExample;
import com.island.ohara.testing.WithBroker;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

public class TestSumExample extends WithBroker {

  @Test
  public void testCase() {
    final BrokerClient client = BrokerClient.of(testUtil().brokersConnProps());
    final Producer<Row, byte[]> producer =
        Producer.<Row, byte[]>builder()
            .connectionProps(client.connectionProps())
            .keySerializer(Serializer.ROW)
            .valueSerializer(Serializer.BYTES)
            .build();
    final int partitions = 1;
    final short replications = 1;
    String fromTopic = "number-input";
    String toTopic = "sum-output";

    // prepare ohara environment
    Map<String, String> settings = new HashMap<>();
    settings.putIfAbsent(StreamDefinitions.BROKER_DEFINITION.key(), client.connectionProps());
    settings.putIfAbsent(StreamDefinitions.NAME_DEFINITION.key(), methodName());
    settings.putIfAbsent(StreamDefinitions.FROM_TOPICS_DEFINITION.key(), fromTopic);
    settings.putIfAbsent(StreamDefinitions.TO_TOPICS_DEFINITION.key(), toTopic);
    StreamTestUtils.setOharaEnv(settings);
    StreamTestUtils.createTopic(client, fromTopic, partitions, replications);
    StreamTestUtils.createTopic(client, toTopic, partitions, replications);
    // prepare data
    List<Row> rows =
        Stream.of(1, 2, 14, 17, 36, 99)
            .map(v -> Row.of(Cell.of("number", v)))
            .collect(Collectors.toList());
    StreamTestUtils.produceData(producer, rows, fromTopic);

    // run example
    SumExample app = new SumExample();
    StreamApp.runStreamApp(app.getClass());

    // Assert the result
    List<Row> expected =
        Stream.of(
                Row.of(Cell.of("dummy", 1), Cell.of("number", 1)),
                Row.of(Cell.of("dummy", 1), Cell.of("number", 18)),
                Row.of(Cell.of("dummy", 1), Cell.of("number", 117)))
            .collect(Collectors.toList());
    StreamTestUtils.assertResult(client, toTopic, expected, 3);
  }
}
