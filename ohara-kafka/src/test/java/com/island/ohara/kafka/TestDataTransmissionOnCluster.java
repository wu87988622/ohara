package com.island.ohara.kafka;

import com.island.ohara.client.ConnectorClient;
import com.island.ohara.client.ConnectorClient$;
import com.island.ohara.client.ConnectorJson;
import com.island.ohara.common.data.Cell;
import com.island.ohara.common.data.Column;
import com.island.ohara.common.data.ConnectorState;
import com.island.ohara.common.data.DataType;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Serializer;
import com.island.ohara.common.util.ByteUtil;
import com.island.ohara.common.util.CommonUtil;
import com.island.ohara.common.util.ReleaseOnce;
import com.island.ohara.integration.OharaTestUtil;
import com.island.ohara.integration.With3Brokers3Workers;
import com.island.ohara.kafka.connector.Constants;
import com.island.ohara.kafka.connector.SimpleRowSinkConnector;
import com.island.ohara.kafka.connector.SimpleRowSourceConnector;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Test;
import scala.Tuple2;
import scala.collection.JavaConversions;
import scala.collection.Seq;

public class TestDataTransmissionOnCluster extends With3Brokers3Workers {

  private OharaTestUtil testUtil = testUtil();
  private ConnectorClient connectorClient =
      ConnectorClient$.MODULE$.apply(testUtil.workersConnProps());

  private KafkaClient kafkaClient = KafkaClient.of(testUtil.brokersConnProps());
  private Row row = Row.of(Cell.of("cf0", 10), Cell.of("cf1", 11));
  private List<Column> schema = Collections.singletonList(Column.of("cf", DataType.BOOLEAN, 1));
  private int numberOfRows = 20;
  // ---------------------------------

  private void createTopic(String topicName, Boolean compacted) {
    if (compacted)
      kafkaClient
          .topicCreator()
          .compacted()
          .numberOfPartitions(1)
          .numberOfReplications((short) 1)
          .create(topicName);
    else
      kafkaClient
          .topicCreator()
          .deleted()
          .numberOfPartitions(1)
          .numberOfReplications((short) 1)
          .create(topicName);
  }

  private void checkData(String topicName) {
    try (Consumer<byte[], Row> consumer =
        Consumer.builder()
            .offsetFromBegin()
            .brokers(testUtil.brokersConnProps())
            .topicName(topicName)
            .build(Serializer.BYTES, Serializer.ROW)) {
      List<ConsumerRecord<byte[], Row>> data = consumer.poll(Duration.ofSeconds(30), numberOfRows);
      assertEquals(data.size(), numberOfRows);
      data.forEach(x -> assertEquals(x.value().get(), row));
    }
  }

  private void setupData(String topicName) {

    try (Producer<byte[], Row> producer =
        Producer.builder()
            .brokers(testUtil.brokersConnProps())
            .build(Serializer.BYTES, Serializer.ROW); ) {
      Stream.iterate(0, i -> i + 1)
          .limit(numberOfRows)
          .forEach(x -> producer.sender().key(ByteUtil.toBytes("key")).value(row).send(topicName));
    }
    checkData(topicName);
  }

  private void checkConnector(String name) {
    CommonUtil.await(
        () -> connectorClient.activeConnectors().contains(name), Duration.ofSeconds(30));
    CommonUtil.await(
        () -> connectorClient.config(name).topics().nonEmpty(), Duration.ofSeconds(30));
    CommonUtil.await(
        () -> {
          try {
            return connectorClient.status(name).connector().state() == ConnectorState.RUNNING;
          } catch (Throwable t) {
            return false;
          }
        },
        Duration.ofSeconds(30));
  }

  @After
  public void tearDown() {
    ReleaseOnce.close(kafkaClient);
  }

  @Test
  public void testRowProducer2RowConsumer() {
    String topicName = methodName();
    // test deleted topic
    createTopic(topicName, false);
    testRowProducer2RowConsumer(topicName);

    topicName = methodName() + "-2";
    // test compacted topic
    createTopic(topicName, true);
    testRowProducer2RowConsumer(topicName);
  }

  /** producer -> topic_1(topicName) -> consumer */
  private void testRowProducer2RowConsumer(String topicName) {
    setupData(topicName);
    try (Consumer<byte[], Row> consumer =
        Consumer.builder()
            .brokers(testUtil.brokersConnProps())
            .offsetFromBegin()
            .topicName(topicName)
            .build(Serializer.BYTES, Serializer.ROW)) {
      List<ConsumerRecord<byte[], Row>> data = consumer.poll(Duration.ofSeconds(10), numberOfRows);
      assertEquals(data.size(), numberOfRows);
      data.forEach(x -> assertEquals(x.value().get(), row));
    }
  }

  @Test
  public void testProducer2SinkConnector() {

    String topicName = methodName();
    String topicName2 = methodName() + "-2";
    // test deleted topic
    createTopic(topicName, false);
    createTopic(topicName2, false);
    testProducer2SinkConnector(topicName, topicName2);

    topicName = methodName() + "-3";
    topicName2 = methodName() + "-4";
    // test compacted topic
    createTopic(topicName, true);
    createTopic(topicName2, true);
    testProducer2SinkConnector(topicName, topicName2);
  }

  /** producer -> topic_1(topicName) -> sink connector -> topic_2(topicName2) */
  private void testProducer2SinkConnector(String topicName, String topicName2) {
    Map<String, String> configs = new HashMap<>();
    configs.put(Constants.BROKER, testUtil.brokersConnProps());
    configs.put(Constants.OUTPUT, topicName2);

    String connectorName = methodName();
    connectorClient
        .connectorCreator()
        .name(connectorName)
        .connectorClass(SimpleRowSinkConnector.class)
        .topic(topicName)
        .numberOfTasks(2)
        .disableConverter()
        .schema(toScalaList(schema))
        .configs(toScalaMap(configs))
        .create();

    try {
      checkConnector(connectorName);
      setupData(topicName);
      checkData(topicName2);
    } finally {
      connectorClient.delete(connectorName);
    }
  }

  @Test
  public void testSourceConnector2Consumer() {
    String topicName = methodName();
    String topicName2 = methodName() + "-2";
    // test deleted topic
    createTopic(topicName, false);
    createTopic(topicName2, false);
    testSourceConnector2Consumer(topicName, topicName2);

    topicName = methodName() + "-3";
    topicName2 = methodName() + "-4";
    // test compacted topic
    createTopic(topicName, true);
    createTopic(topicName2, true);
    testSourceConnector2Consumer(topicName, topicName2);
  }

  /** producer -> topic_1(topicName) -> row source -> topic_2 -> consumer */
  private void testSourceConnector2Consumer(String topicName, String topicName2) {
    Map<String, String> configs = new HashMap<>();
    configs.put(Constants.BROKER, testUtil.brokersConnProps());
    configs.put(Constants.INPUT, topicName);

    String connectorName = methodName();
    connectorClient
        .connectorCreator()
        .name(connectorName)
        .connectorClass(SimpleRowSourceConnector.class)
        .topic(topicName2)
        .numberOfTasks(2)
        .disableConverter()
        .schema(toScalaList(schema))
        .configs(toScalaMap(configs))
        .create();

    try {
      checkConnector(connectorName);
      setupData(topicName);
      checkData(topicName2);
    } finally {
      connectorClient.delete(connectorName);
    }
  }

  /** Test case for OHARA-150 */
  @Test
  public void shouldKeepColumnOrderAfterSendToKafka()
      throws ExecutionException, InterruptedException {
    String topicName = methodName();
    KafkaUtil.createTopic(testUtil.brokersConnProps(), topicName, 1, (short) 1);

    Row row = Row.of(Cell.of("c", 3), Cell.of("b", 2), Cell.of("a", 1));

    try (Producer<String, Row> producer =
        Producer.builder()
            .brokers(testUtil.brokersConnProps())
            .build(Serializer.STRING, Serializer.ROW)) {
      producer.sender().key(topicName).value(row).send(topicName);
      producer.flush();
    }

    try (Consumer<String, Row> consumer =
        Consumer.builder()
            .brokers(testUtil.brokersConnProps())
            .offsetFromBegin()
            .topicName(topicName)
            .build(Serializer.STRING, Serializer.ROW)) {
      List<ConsumerRecord<String, Row>> fromKafka = consumer.poll(Duration.ofSeconds(30), 1);

      assertFalse(fromKafka.isEmpty());
      Row row2 = fromKafka.get(0).value().get();
      assertEquals(row2.cell(0).name(), "c");
      assertEquals(row2.cell(1).name(), "b");
      assertEquals(row2.cell(2).name(), "a");
    }

    try (Producer<String, Row> producer =
        Producer.builder()
            .brokers(testUtil.brokersConnProps())
            .build(Serializer.STRING, Serializer.ROW)) {
      RecordMetadata meta = producer.sender().key(topicName).value(row).send(topicName).get();
      assertEquals(meta.topic(), topicName);
    }
  }

  /**
   * Test for ConnectorClient
   *
   * @see ConnectorClient
   */
  @Test
  public void connectorClientTest() {
    String connectorName = methodName();
    List<String> topics = Arrays.asList(connectorName + "_topic", connectorName + "_topic2");
    String output_topic = connectorName + "_topic_output";

    Map<String, String> configs = new HashMap<>();
    configs.put(Constants.BROKER, testUtil.brokersConnProps());
    configs.put(Constants.OUTPUT, output_topic);
    connectorClient
        .connectorCreator()
        .name(connectorName)
        .connectorClass(SimpleRowSinkConnector.class)
        .topics(toScalaList(topics))
        .numberOfTasks(2)
        .disableConverter()
        .schema(toScalaList(schema))
        .configs(toScalaMap(configs))
        .create();

    Seq<String> activeConnectors = connectorClient.activeConnectors();
    assertTrue(activeConnectors.contains(connectorName));

    ConnectorJson.ConnectorConfig config = connectorClient.config(connectorName);
    assertEquals(config.topics(), toScalaList(topics));

    CommonUtil.await(
        () -> connectorClient.status(connectorName).tasks().size() > 0, Duration.ofSeconds(10));
    ConnectorJson.ConnectorInformation status = connectorClient.status(connectorName);
    assertNotNull(status.tasks().head());

    ConnectorJson.TaskStatus task =
        connectorClient.taskStatus(connectorName, status.tasks().head().id());
    assertNotNull(task);
    assertEquals(task, status.tasks().head());
    assertFalse(task.worker_id().isEmpty());

    connectorClient.delete(connectorName);
    assertFalse(connectorClient.activeConnectors().contains(connectorName));
  }

  /**
   * To scala map - befroe Connecter change to scala version
   *
   * @param javaMap
   * @param <K>
   * @param <V>
   * @return
   */
  @SuppressWarnings("unchecked")
  private static <K, V> scala.collection.immutable.Map<K, V> toScalaMap(
      java.util.Map<K, V> javaMap) {
    final java.util.List<scala.Tuple2<K, V>> list = new java.util.ArrayList<>(javaMap.size());
    for (final java.util.Map.Entry<K, V> entry : javaMap.entrySet()) {
      list.add(scala.Tuple2.apply(entry.getKey(), entry.getValue()));
    }
    final scala.collection.Seq<Tuple2<K, V>> seq =
        scala.collection.JavaConverters.asScalaBufferConverter(list).asScala().toSeq();
    return (scala.collection.immutable.Map<K, V>)
        scala.collection.immutable.Map$.MODULE$.apply(seq);
  }

  private <E> Seq<E> toScalaList(List<E> list) {
    return JavaConversions.asScalaBuffer(list);
  }
}
