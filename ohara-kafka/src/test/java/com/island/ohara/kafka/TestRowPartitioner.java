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

import com.island.ohara.common.data.Cell;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Serializer;
import com.island.ohara.common.rule.OharaTest;
import com.island.ohara.common.setting.TopicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.junit.Assert;
import org.junit.Test;

public class TestRowPartitioner extends OharaTest {

  @Test
  public void nonRowShouldNotBePassedToSubClass() {
    AtomicInteger count = new AtomicInteger(0);
    RowPartitioner custom =
        new RowPartitioner() {
          @Override
          public Optional<Integer> partition(
              TopicKey topicKey, Row row, byte[] serializedRow, Cluster cluster) {
            count.incrementAndGet();
            return Optional.empty();
          }
        };
    Node node = new Node(0, "localhost", 99);
    Node[] nodes = new Node[] {node};
    TopicKey key = TopicKey.of("a", "b");
    PartitionInfo partitionInfo = new PartitionInfo(key.topicNameOnKafka(), 1, node, nodes, nodes);
    org.apache.kafka.common.Cluster cluster =
        new org.apache.kafka.common.Cluster(
            "aa",
            Arrays.asList(nodes),
            Collections.singletonList(partitionInfo),
            Collections.emptySet(),
            Collections.emptySet());
    custom.partition(key.topicNameOnKafka(), "sss", "sss".getBytes(), null, null, cluster);
    Assert.assertEquals(0, count.get());

    Row row = Row.of(Cell.of("a", "b"));
    custom.partition(key.topicNameOnKafka(), row, Serializer.ROW.to(row), null, null, cluster);
    Assert.assertEquals(1, count.get());
  }
}
