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
import com.island.ohara.kafka.BrokerClient;
import com.island.ohara.kafka.Consumer;
import com.island.ohara.kafka.Consumer.Record;
import com.island.ohara.kafka.Producer;
import com.island.ohara.streams.OStream;
import com.island.ohara.streams.StreamApp;
import com.island.ohara.testing.WithBroker;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"rawtypes"})
public class TestPurchaseAnalysis extends WithBroker {
  private static final Logger LOG = LoggerFactory.getLogger(TestPurchaseAnalysis.class);
  private static final String appid = "test-purchase-analysis";
  private static final String resultTopic = "gender-amount";
  private static final String itemTopic = "items";
  private static final String orderTopic = "orders";
  private static final String userTopic = "users";
  private final BrokerClient client = BrokerClient.of(testUtil().brokersConnProps());
  private final Producer<Row, byte[]> producer =
      Producer.<Row, byte[]>builder()
          .connectionProps(client.connectionProps())
          .keySerializer(Serializer.ROW)
          .valueSerializer(Serializer.BYTES)
          .build();

  @Before
  public void setup() {
    int partitions = 1;
    short replications = 1;
    try {
      client
          .topicCreator()
          .numberOfPartitions(partitions)
          .numberOfReplications(replications)
          .topicName(orderTopic)
          .create();
      client
          .topicCreator()
          .numberOfPartitions(partitions)
          .numberOfReplications(replications)
          .topicName(itemTopic)
          .create();
      client
          .topicCreator()
          .numberOfPartitions(partitions)
          .numberOfReplications(replications)
          .topicName(userTopic)
          .create();
      client
          .topicCreator()
          .numberOfPartitions(partitions)
          .numberOfReplications(replications)
          .topicName(resultTopic)
          .create();
    } catch (Exception e) {
      LOG.error(e.getMessage());
    }

    // write users.csv to kafka broker
    produceData("users.csv", userTopic);

    // write items.csv to kafka broker
    produceData("items.csv", itemTopic);

    // write orders.csv to kafka broker
    produceData("orders.csv", orderTopic);
  }

  @Test
  public void testStreamApp() {
    RunStreamApp app = new RunStreamApp(client.connectionProps());
    StreamApp.runStreamApp(app.getClass(), client.connectionProps());

    Consumer<Row, byte[]> consumer =
        Consumer.<Row, byte[]>builder()
            .topicName(resultTopic)
            .connectionProps(client.connectionProps())
            .groupId("group-" + resultTopic)
            .offsetFromBegin()
            .keySerializer(Serializer.ROW)
            .valueSerializer(Serializer.BYTES)
            .build();

    List<Record<Row, byte[]>> records = consumer.poll(Duration.ofSeconds(100), 2);
    // the result will get "accumulation" ; hence we will get 2 -> 4 records
    Assert.assertTrue(
        "the result will get \"accumulation\" ; hence we will get 2 -> 4 records. actual:"
            + records.size(),
        records.size() >= 2 && records.size() <= 4);

    Map<String, Double[]> actualResultMap = new HashMap<>();
    actualResultMap.put("male", new Double[] {60000D, 69000D});
    actualResultMap.put("female", new Double[] {15000D, 45000D});
    final double THRESHOLD = 0.0001;

    records.forEach(
        record -> {
          if (record.key().isPresent()) {
            Optional<Double> amount =
                record.key().get().cells().stream()
                    .filter(cell -> cell.name().equals("amount"))
                    .map(cell -> Double.valueOf(cell.value().toString()))
                    .findFirst();
            Assert.assertTrue(
                "the result should be contain in actualResultMap",
                actualResultMap.containsKey(record.key().get().cell("gender").value().toString())
                    && actualResultMap.values().stream()
                        .flatMap(Arrays::stream)
                        .anyMatch(d -> Math.abs(d - amount.orElse(-999.0)) < THRESHOLD));
          }
        });

    consumer.close();
  }

  @After
  public void cleanUp() {
    producer.close();
    client.close();
  }

  /** StreamApp Main Entry */
  public static class RunStreamApp extends StreamApp {

    final String brokers;

    public RunStreamApp(String brokers) {
      this.brokers = brokers;
    }

    @Override
    public void start() {
      OStream<Row> ostream =
          OStream.builder()
              .appid(appid)
              .bootstrapServers(brokers)
              .fromTopicWith(orderTopic, Serdes.ROW, Serdes.BYTES)
              .toTopicWith(resultTopic, Serdes.ROW, Serdes.BYTES)
              .cleanStart()
              .timestampExactor(MyExtractor.class)
              .build();

      ostream
          .leftJoin(
              userTopic,
              Conditions.add(Collections.singletonList(Pair.of("userName", "name"))),
              (row1, row2) ->
                  Row.of(
                      row1.cell("userName"),
                      row1.cell("itemName"),
                      row1.cell("quantity"),
                      row2 == null ? Cell.of("address", "") : row2.cell("address"),
                      row2 == null ? Cell.of("gender", "") : row2.cell("gender")))
          .filter(row -> row.cell("address").value() != null)
          .leftJoin(
              itemTopic,
              Conditions.add(Collections.singletonList(Pair.of("itemName", "itemName"))),
              (row1, row2) ->
                  Row.of(
                      row1.cell("userName"),
                      row1.cell("itemName"),
                      row1.cell("quantity"),
                      Cell.of("useraddress", row1.cell("address").value()),
                      row1.cell("gender"),
                      row2 == null
                          ? Cell.of("itemaddress", "")
                          : Cell.of("itemaddress", row2.cell("address").value()),
                      row2 == null ? Cell.of("type", "") : row2.cell("type"),
                      row2 == null ? Cell.of("price", "") : row2.cell("price")))
          .filter(
              row ->
                  row.cell("useraddress")
                      .value()
                      .toString()
                      .equals(row.cell("itemaddress").value().toString()))
          .map(
              row ->
                  Row.of(
                      row.cell("gender"),
                      Cell.of(
                          "amount",
                          Double.valueOf(row.cell("quantity").value().toString())
                              * Double.valueOf(row.cell("price").value().toString()))))
          .groupByKey(Collections.singletonList("gender"))
          .reduce(
              (r1, r2) ->
                  Row.of(
                      Cell.of(
                          "amount",
                          Double.valueOf(r1.cell("amount").value().toString())
                              + Double.valueOf(r2.cell("amount").value().toString()))))
          .start();
    }
  }

  public static class MyExtractor implements TimestampExtractor {

    private final DateTimeFormatter dataTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public long extract(
        org.apache.kafka.clients.consumer.ConsumerRecord<Object, Object> record,
        long previousTimestamp) {
      Object value = record.value();
      if (value instanceof Row) {
        Row row = (Row) value;
        // orders
        if (row.names().contains("transactionDate"))
          return LocalDateTime.parse(
                      row.cell("transactionDate").value().toString(), dataTimeFormatter)
                  .toEpochSecond(ZoneOffset.UTC)
              * 1000;
        // items
        if (row.names().contains("price"))
          return LocalDateTime.of(2015, 12, 11, 1, 0, 10).toEpochSecond(ZoneOffset.UTC) * 1000;
        // users
        else if (row.names().contains("gender"))
          return LocalDateTime.of(2015, 12, 11, 0, 0, 10).toEpochSecond(ZoneOffset.UTC) * 1000;
        // other
        else return LocalDateTime.of(2015, 12, 11, 2, 0, 10).toEpochSecond(ZoneOffset.UTC) * 1000;
      } else {
        return LocalDateTime.of(2015, 11, 10, 0, 0, 10).toEpochSecond(ZoneOffset.UTC) * 1000;
      }
    }
  }

  private void produceData(String filename, String topicName) {
    try {
      List<?> dataList = DataUtils.readData(filename);
      dataList.stream()
          .map(
              object -> {
                try {
                  List<Cell> cells = new ArrayList<>();
                  LOG.debug("Class Name : " + object.getClass().getName());
                  for (Field f : object.getClass().getDeclaredFields()) {
                    f.setAccessible(true);
                    Cell cell = Cell.of(f.getName(), f.get(object));
                    cells.add(cell);
                    LOG.debug("--" + f.getName() + ":" + f.get(object));
                  }
                  return new AbstractMap.SimpleEntry<>(
                      Row.of(cells.toArray(new Cell[0])), new byte[0]);
                } catch (Exception e) {
                  LOG.debug(e.getMessage());
                  return new AbstractMap.SimpleEntry<>(Row.EMPTY, new byte[0]);
                }
              })
          .forEach(
              entry ->
                  producer
                      .sender()
                      .key(entry.getKey())
                      .value(entry.getValue())
                      .topicName(topicName)
                      .send());
    } catch (Exception e) {
      LOG.debug(e.getMessage());
    }
  }
}
