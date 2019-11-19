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

import com.island.ohara.common.data.Pair;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.rule.OharaTest;
import com.island.ohara.common.setting.TopicKey;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.streams.OStream;
import com.island.ohara.streams.Stream;
import com.island.ohara.streams.config.StreamDefUtils;
import com.island.ohara.streams.config.StreamSetting;
import com.island.ohara.streams.data.Poneglyph;
import com.island.ohara.streams.data.Stele;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

public class TestStreamTopology extends OharaTest {

  private static String appId = CommonUtils.randomString(5);
  private static TopicKey fromKey =
      TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString());
  private static TopicKey toKey =
      TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString());
  private static String join = "join_topic";

  @Test
  public void testGetTopology() {
    DescribeStream app = new DescribeStream();
    Stream.execute(
        app.getClass(),
        java.util.stream.Stream.of(
                Pair.of(StreamDefUtils.NAME_DEFINITION.key(), appId),
                Pair.of(StreamDefUtils.BROKER_DEFINITION.key(), "fake"),
                Pair.of(
                    StreamDefUtils.FROM_TOPIC_KEYS_DEFINITION.key(),
                    TopicKey.toJsonString(Collections.singletonList(fromKey))),
                Pair.of(
                    StreamDefUtils.TO_TOPIC_KEYS_DEFINITION.key(),
                    TopicKey.toJsonString(Collections.singletonList(toKey))))
            .collect(Collectors.toMap(Pair::left, Pair::right)));
  }

  public static class DescribeStream extends Stream {

    @Override
    public void start(OStream<Row> ostream, StreamSetting streamSetting) {
      List<Poneglyph> poneglyph =
          ostream
              .filter(row -> !row.cell(0).value().toString().isEmpty())
              .map(row -> Row.of(row.cell(0)))
              .leftJoin(
                  join,
                  Conditions.create().add(Collections.singletonList(Pair.of("pk", "fk"))),
                  (r1, r2) -> r1)
              .groupByKey(Collections.singletonList("key"))
              .count()
              .getPoneglyph();

      // It should have four "steles", i.e., four process topology
      Assert.assertEquals(4, poneglyph.size());

      // Topics should be contained in topologies
      Arrays.asList(fromKey.topicNameOnKafka(), toKey.topicNameOnKafka(), join)
          .forEach(
              topic ->
                  Assert.assertTrue(
                      "all used topics should be contained in topologies",
                      poneglyph.stream()
                          .flatMap(p -> p.getSteles().stream())
                          .filter(s -> s.getKind().equals("Source") || s.getKind().equals("Sink"))
                          .map(Stele::getName)
                          .anyMatch(topologyTopic -> topologyTopic.contains(topic))));
    }
  }
}
