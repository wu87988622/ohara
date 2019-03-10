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
import com.island.ohara.kafka.Consumer;
import com.island.ohara.kafka.Consumer.Record;
import com.island.ohara.kafka.Producer;
import com.island.ohara.streams.ostream.KeyValue;
import com.island.ohara.streams.ostream.Serdes;
import com.island.ohara.streams.ostream.TimestampExtractor;
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
  private static final String orderuser_repartition = "orderuser-repartition-by-item";
  private final BrokerClient client = BrokerClient.of(testUtil().brokersConnProps());
  private final Producer<String, Row> producer =
      Producer.<String, Row>builder()
          .connectionProps(client.connectionProps())
          .keySerializer(Serializer.STRING)
          .valueSerializer(Serializer.ROW)
          .build();

  @Before
  public void prepareData() {

    int partitions = 1;
    short replications = 1;
    try {
      client
          .topicCreator()
          .numberOfPartitions(partitions)
          .numberOfReplications(replications)
          .create(orderTopic);
      client
          .topicCreator()
          .numberOfPartitions(partitions)
          .numberOfReplications(replications)
          .create(itemTopic);
      client
          .topicCreator()
          .numberOfPartitions(partitions)
          .numberOfReplications(replications)
          .create(userTopic);
      client
          .topicCreator()
          .numberOfPartitions(partitions)
          .numberOfReplications(replications)
          .create(resultTopic);
      client
          .topicCreator()
          .numberOfPartitions(partitions)
          .numberOfReplications(replications)
          .create(orderuser_repartition);
    } catch (Exception e) {
      LOG.error(e.getMessage());
    }

    // write users.csv to kafka broker
    produceData("users.csv", userTopic, "name");

    // write items.csv to kafka broker
    produceData("items.csv", itemTopic, "itemName");

    // write orders.csv to kafka broker
    produceData("orders.csv", orderTopic, "userName");
  }

  @Test
  public void testStreamApp() {
    RunStreamApp app = new RunStreamApp(client.connectionProps());
    StreamApp.runStreamApp(app.getClass(), client.connectionProps());

    Consumer<String, Double> consumer =
        Consumer.<String, Double>builder()
            .topicName(resultTopic)
            .connectionProps(client.connectionProps())
            .groupId("group-" + resultTopic)
            .offsetFromBegin()
            .keySerializer(Serializer.STRING)
            .valueSerializer(Serializer.DOUBLE)
            .build();

    List<Record<String, Double>> records = consumer.poll(Duration.ofSeconds(10), 2);
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
            Assert.assertTrue(
                "the result should be contain in actualResultMap",
                actualResultMap.containsKey(record.key().get())
                    && actualResultMap
                        .values()
                        .stream()
                        .flatMap(doubles -> Arrays.stream(doubles))
                        .anyMatch((d) -> Math.abs(d - record.value().get()) < THRESHOLD));
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
      OStream<String, Row> ostream =
          OStream.builder()
              .appid(appid)
              .bootstrapServers(brokers)
              .fromTopicWith(orderTopic, Serdes.STRING, Serdes.ROW)
              .toTopicWith(resultTopic, Serdes.STRING, Serdes.DOUBLE)
              .cleanStart()
              .timestampExactor(MyExtractor.class)
              .build();

      ostream
          .leftJoin(
              userTopic,
              Serdes.STRING,
              Serdes.ROW,
              (row1, row2) -> {
                Row newRow =
                    Row.of(
                        row1.cell("userName"),
                        row1.cell("itemName"),
                        row1.cell("transactionDate"),
                        row1.cell("quantity"),
                        row2 == null ? Cell.of("address", "") : row2.cell("address"),
                        row2 == null ? Cell.of("gender", "") : row2.cell("gender"),
                        row2 == null ? Cell.of("age", "") : row2.cell("age"));
                return newRow;
              })
          .filter((key, row) -> row.cell("address").value() != null)
          .map((key, row) -> new KeyValue<>(row.cell("itemName").value().toString(), row))
          .through(orderuser_repartition, Serdes.STRING, Serdes.ROW)
          .leftJoin(
              itemTopic,
              Serdes.STRING,
              Serdes.ROW,
              (row1, row2) -> {
                Row newRow =
                    Row.of(
                        row1.cell("userName"),
                        row1.cell("itemName"),
                        row1.cell("transactionDate"),
                        row1.cell("quantity"),
                        Cell.of("useraddress", row1.cell("address").value()),
                        row1.cell("gender"),
                        row1.cell("age"),
                        row2 == null
                            ? Cell.of("itemaddress", "")
                            : Cell.of("itemaddress", row2.cell("address").value()),
                        row2 == null ? Cell.of("type", "") : row2.cell("type"),
                        row2 == null ? Cell.of("price", "") : row2.cell("price"));
                return newRow;
              })
          .filter(
              (key, row) ->
                  row.cell("useraddress")
                      .value()
                      .toString()
                      .equals(row.cell("itemaddress").value().toString()))
          .map(
              (key, row) ->
                  new KeyValue<>(
                      row.cell("gender").value().toString(),
                      Double.valueOf(row.cell("quantity").value().toString())
                          * Double.valueOf(row.cell("price").value().toString())))
          .groupByKey(Serdes.STRING, Serdes.DOUBLE)
          .reduce((v1, v2) -> v1 + v2)
          .toOStream()
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

  private void produceData(String filename, String topicName, String topicKey) {
    try {
      List<?> dataList = DataImporter.readData(filename);
      dataList
          .stream()
          .map(
              object -> {
                try {
                  List<Cell> cells = new ArrayList<>();
                  LOG.debug("Class Name : " + object.getClass().getName());
                  String key = null;
                  for (Field f : object.getClass().getDeclaredFields()) {
                    f.setAccessible(true);
                    Cell cell = Cell.of(f.getName(), f.get(object));
                    cells.add(cell);
                    if (cell.name().equals(topicKey)) key = cell.value().toString();
                    LOG.debug("--" + f.getName() + ":" + f.get(object));
                  }
                  return new AbstractMap.SimpleEntry<>(key, Row.of(cells.toArray(new Cell[0])));
                } catch (Exception e) {
                  LOG.debug(e.getMessage());
                  return new AbstractMap.SimpleEntry<>(null, Row.EMPTY);
                }
              })
          .forEach(
              entry -> {
                producer
                    .sender()
                    .key(entry.getKey().toString())
                    .value(entry.getValue())
                    .topicName(topicName)
                    .send();
              });
    } catch (Exception e) {
      LOG.debug(e.getMessage());
    }
  }
}
