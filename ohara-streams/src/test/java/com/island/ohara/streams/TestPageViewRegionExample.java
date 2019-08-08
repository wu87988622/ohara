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
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.BrokerClient;
import com.island.ohara.kafka.Producer;
import com.island.ohara.streams.config.StreamDefinitions.DefaultConfigs;
import com.island.ohara.streams.examples.PageViewRegionExample;
import com.island.ohara.testing.WithBroker;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPageViewRegionExample extends WithBroker {

  private final BrokerClient client = BrokerClient.of(testUtil().brokersConnProps());
  private final Producer<Row, byte[]> producer =
      Producer.<Row, byte[]>builder()
          .connectionProps(client.connectionProps())
          .keySerializer(Serializer.ROW)
          .valueSerializer(Serializer.BYTES)
          .build();
  private final String fromTopic = "page-views";
  private final String joinTableTopic = "user-profiles";
  private final String toTopic = "view-by-region";

  // prepare data
  private final List<Row> views =
      Stream.of(
              Row.of(Cell.of("user", "francesca"), Cell.of("page", "http://example.com/#bell")),
              Row.of(Cell.of("user", "eden"), Cell.of("page", "https://baseball.example.com/")),
              Row.of(Cell.of("user", "abbie"), Cell.of("page", "https://www.example.com/")),
              Row.of(
                  Cell.of("user", "aisha"),
                  Cell.of("page", "http://www.example.net/beginner/brother")),
              Row.of(Cell.of("user", "eden"), Cell.of("page", "http://www.example.net/")),
              Row.of(
                  Cell.of("user", "tommy"), Cell.of("page", "https://attack.example.org/amount")),
              Row.of(
                  Cell.of("user", "aisha"),
                  Cell.of(
                      "page", "http://www.example.org/afterthought.html?addition=base&angle=art")),
              Row.of(Cell.of("user", "elsa"), Cell.of("page", "https://belief.example.com/")),
              Row.of(
                  Cell.of("user", "abbie"),
                  Cell.of("page", "https://example.com/blade.php?berry=bike&action=boot#airplane")),
              Row.of(Cell.of("user", "elsa"), Cell.of("page", "http://example.com/")),
              Row.of(Cell.of("user", "eden"), Cell.of("page", "http://example.com/")),
              Row.of(Cell.of("user", "tommy"), Cell.of("page", "http://example.com/")),
              Row.of(Cell.of("user", "aisha"), Cell.of("page", "http://www.example.com/bead")),
              Row.of(Cell.of("user", "tommy"), Cell.of("page", "http://angle.example.com/")),
              Row.of(Cell.of("user", "tiffany"), Cell.of("page", "http://example.com/birds.html")),
              Row.of(
                  Cell.of("user", "abbie"),
                  Cell.of("page", "http://www.example.org/bubble/aunt.html")),
              Row.of(
                  Cell.of("user", "elsa"),
                  Cell.of("page", "https://example.com/?baseball=bat&birds=beef")),
              Row.of(
                  Cell.of("user", "tiffany"),
                  Cell.of(
                      "page", "http://amusement.example.com/?behavior=believe&brass=ball#basket")),
              Row.of(
                  Cell.of("user", "abbie"),
                  Cell.of(
                      "page",
                      "https://www.example.net/afternoon/balance.php?beef=blow&bee=advertisement")),
              Row.of(
                  Cell.of("user", "francesca"),
                  Cell.of("page", "http://www.example.com/?bike=ants&airplane=action")))
          .collect(Collectors.toList());

  private final List<Row> profiles =
      Stream.of(
              Row.of(Cell.of("user", "abbie"), Cell.of("region", "Russian")),
              Row.of(Cell.of("user", "tommy"), Cell.of("region", "Jordan")),
              Row.of(Cell.of("user", "francesca"), Cell.of("region", "Belize")),
              Row.of(Cell.of("user", "eden"), Cell.of("region", "Russian")),
              Row.of(Cell.of("user", "tiffany"), Cell.of("region", "Jordan")),
              Row.of(Cell.of("user", "aisha"), Cell.of("region", "Russian")),
              Row.of(Cell.of("user", "elsa"), Cell.of("region", "Cuba")))
          .collect(Collectors.toList());

  @Before
  public void setup() {
    final int partitions = 1;
    final short replications = 1;
    String appId = CommonUtils.randomString();

    // prepare ohara environment
    Map<String, String> settings = new HashMap<>();
    settings.putIfAbsent(DefaultConfigs.BROKER_DEFINITION.key(), client.connectionProps());
    settings.putIfAbsent(DefaultConfigs.NAME_DEFINITION.key(), appId);
    settings.putIfAbsent(DefaultConfigs.FROM_TOPICS_DEFINITION.key(), fromTopic);
    settings.putIfAbsent(DefaultConfigs.TO_TOPICS_DEFINITION.key(), toTopic);
    settings.putIfAbsent("joinTopic", joinTableTopic);
    StreamTestUtils.setOharaEnv(settings);

    StreamTestUtils.createTopic(client, fromTopic, partitions, replications);
    StreamTestUtils.createTopic(client, joinTableTopic, partitions, replications);
    StreamTestUtils.createTopic(client, toTopic, partitions, replications);
  }

  @Test
  public void testCase() throws InterruptedException {
    // run example
    PageViewRegionExample app = new PageViewRegionExample();
    StreamApp.runStreamApp(app.getClass());

    StreamTestUtils.produceData(producer, profiles, joinTableTopic);
    // the default commit.interval.ms=30 seconds, which should make sure join table ready
    TimeUnit.SECONDS.sleep(30);
    StreamTestUtils.produceData(producer, views, fromTopic);

    // Assert the result
    List<Row> expected =
        Stream.of(
                Row.of(Cell.of("region", "Belize"), Cell.of("count", 2L)),
                Row.of(Cell.of("region", "Russian"), Cell.of("count", 10L)),
                Row.of(Cell.of("region", "Jordan"), Cell.of("count", 5L)),
                Row.of(Cell.of("region", "Cuba"), Cell.of("count", 3L)))
            .collect(Collectors.toList());
    StreamTestUtils.assertResult(client, toTopic, expected, 20);
  }

  @After
  public void cleanUp() {
    producer.close();
    client.close();
  }
}
