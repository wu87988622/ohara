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
import com.island.ohara.streams.examples.WordCountExample;
import com.island.ohara.testing.WithBroker;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

public class TestWordCountExample extends WithBroker {

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
    String fromTopic = "text-input";
    String toTopic = "count-output";

    // prepare ohara environment
    StreamTestUtils.setOharaEnv(client.connectionProps(), methodName(), fromTopic, toTopic);
    StreamTestUtils.createTopic(client, fromTopic, partitions, replications);
    StreamTestUtils.createTopic(client, toTopic, partitions, replications);
    // prepare data
    List<Row> rows =
        Stream.of("hello", "ohara", "stream", "world", "of", "stream")
            .map(str -> Row.of(Cell.of("word", str)))
            .collect(Collectors.toList());
    StreamTestUtils.produceData(producer, rows, fromTopic);

    // run example
    WordCountExample app = new WordCountExample();
    StreamApp.runStreamApp(app.getClass());

    // Assert the result
    List<Row> expected =
        Stream.of(
                Row.of(Cell.of("word", "stream"), Cell.of("count", 2L)),
                Row.of(Cell.of("word", "world"), Cell.of("count", 1L)))
            .collect(Collectors.toList());
    // Since the result of "count" is "accumulate", we will get the same size as input count
    StreamTestUtils.assertResult(client, toTopic, expected, rows.size());
  }
}
