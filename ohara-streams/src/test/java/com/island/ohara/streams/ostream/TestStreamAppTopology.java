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

import com.island.ohara.common.data.Row;
import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.streams.OStream;
import com.island.ohara.streams.StreamApp;
import com.island.ohara.streams.data.Poneglyph;
import com.island.ohara.streams.data.Stele;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class TestStreamAppTopology extends SmallTest {

  @Test
  public void testGetTopology() {
    DescribeStreamApp app = new DescribeStreamApp();
    StreamApp.runStreamApp(app.getClass());
  }

  public static class DescribeStreamApp extends StreamApp {
    String from = "from_topic";
    String to = "to_topic";
    String join = "join_topic";

    @Override
    public void start() {
      OStream<Row, String> ostream =
          OStream.builder()
              .fromTopicWith(from, Serdes.ROW, Serdes.STRING)
              .toTopic(to)
              .bootstrapServers("fake")
              .appid("get-poneglyph")
              .build();

      List<Poneglyph> poneglyph =
          ostream
              .filter((row, value) -> !row.cell(0).value().toString().isEmpty())
              .map(
                  (key, value) ->
                      new KeyValue<>(key.cell(0).name(), key.cell(0).value().toString()))
              .leftJoin(join, Serdes.STRING, Serdes.STRING, (r1, r2) -> r1 + r2)
              .groupByKey(Serdes.STRING, Serdes.STRING)
              .count()
              .getPoneglyph();

      // It should have two "steles", i.e., two process topology
      Assert.assertEquals(2, poneglyph.size());

      // Topics should be contained in topologies
      Arrays.asList(from, to, join)
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
