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

package com.island.ohara.client.kafka

import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorState
import com.island.ohara.common.data._
import com.island.ohara.common.setting.{ConnectorKey, TopicKey}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.kafka.{BrokerClient, Consumer, Producer}
import com.island.ohara.testing.WithBrokerWorker
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
class TestDataTransmissionOnCluster extends WithBrokerWorker with Matchers {

  private[this] val brokerClient = BrokerClient.of(testUtil().brokersConnProps)
  private[this] val workerClient = WorkerClient(testUtil().workersConnProps())
  private[this] val row = Row.of(Cell.of("cf0", 10), Cell.of("cf1", 11))
  private[this] val schema = Seq(Column.builder().name("cf").dataType(DataType.BOOLEAN).order(1).build())
  private[this] val numberOfRows = 20

  @After
  def tearDown(): Unit = Releasable.close(brokerClient)

  private[this] def createTopic(topicKey: TopicKey, compacted: Boolean): Unit = {
    if (compacted)
      brokerClient
        .topicCreator()
        .compacted()
        .numberOfPartitions(1)
        .numberOfReplications(1)
        .topicName(topicKey.topicNameOnKafka)
        .create()
    else
      brokerClient
        .topicCreator()
        .deleted()
        .numberOfPartitions(1)
        .numberOfReplications(1)
        .topicName(topicKey.topicNameOnKafka)
        .create()
  }

  private[this] def setupData(topicKey: TopicKey): Unit = {
    val producer = Producer
      .builder[Row, Array[Byte]]()
      .connectionProps(testUtil.brokersConnProps)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    try 0 until numberOfRows foreach (_ => producer.sender().key(row).topicName(topicKey.topicNameOnKafka).send())
    finally producer.close()
    checkData(topicKey)
  }

  private[this] def checkData(topicKey: TopicKey): Unit = {
    val consumer = Consumer
      .builder[Row, Array[Byte]]()
      .offsetFromBegin()
      .connectionProps(testUtil.brokersConnProps)
      .topicName(topicKey.topicNameOnKafka)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    val data = consumer.poll(java.time.Duration.ofSeconds(30), numberOfRows)
    data.size shouldBe numberOfRows
    data.asScala.foreach(_.key.get shouldBe row)
  }

  private[this] def checkConnector(connectorKey: ConnectorKey): Unit = {
    await(() => result(workerClient.activeConnectors()).contains(connectorKey.connectorNameOnKafka()))
    await(() => result(workerClient.config(connectorKey)).topicNames.nonEmpty)
    await(
      () =>
        try result(workerClient.status(connectorKey)).connector.state == ConnectorState.RUNNING
        catch {
          case _: Throwable => false
      })
  }

  @Test
  def testRowProducer2RowConsumer(): Unit = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    //test deleted topic
    createTopic(topicKey, false)
    testRowProducer2RowConsumer(topicKey)

    val topicKey2 = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    //test compacted topic
    createTopic(topicKey2, true)
    testRowProducer2RowConsumer(topicKey2)
  }

  /**
    * producer -> topic_1(topicName) -> consumer
    */
  private[this] def testRowProducer2RowConsumer(topicKey: TopicKey): Unit = {
    setupData(topicKey)
    val consumer = Consumer
      .builder[Row, Array[Byte]]()
      .offsetFromBegin()
      .connectionProps(testUtil.brokersConnProps)
      .topicName(topicKey.topicNameOnKafka)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    try {
      val data = consumer.poll(java.time.Duration.ofSeconds(30), numberOfRows)
      data.size shouldBe numberOfRows
      data.asScala.foreach(_.key.get shouldBe row)
    } finally consumer.close()
  }

  @Test
  def testProducer2SinkConnector(): Unit = {
    val srcKey = TopicKey.of(CommonUtils.randomString(10), CommonUtils.randomString(10))
    val targetKey = TopicKey.of(CommonUtils.randomString(10), CommonUtils.randomString(10))
    //test deleted topic
    createTopic(srcKey, false)
    createTopic(targetKey, false)
    testProducer2SinkConnector(srcKey, targetKey)

    val srcKey2 = TopicKey.of(CommonUtils.randomString(10), CommonUtils.randomString(10))
    val targetKey2 = TopicKey.of(CommonUtils.randomString(10), CommonUtils.randomString(10))
    //test compacted topic
    createTopic(srcKey2, true)
    createTopic(targetKey2, true)
    testProducer2SinkConnector(srcKey2, targetKey2)
  }

  /**
    * producer -> topic_1(topicName) -> sink connector -> topic_2(topicName2)
    */
  private[this] def testProducer2SinkConnector(srcKey: TopicKey, targetKey: TopicKey): Unit = {
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(10), CommonUtils.randomString(10))
    result(
      workerClient
        .connectorCreator()
        .connectorKey(connectorKey)
        .connectorClass(classOf[SimpleRowSinkConnector])
        .topicKey(srcKey)
        .numberOfTasks(1)
        .columns(schema)
        .settings(Map(BROKER -> testUtil.brokersConnProps, OUTPUT -> targetKey.topicNameOnKafka))
        .create())

    try {
      checkConnector(connectorKey)
      setupData(srcKey)
      checkData(targetKey)
    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testSourceConnector2Consumer(): Unit = {
    val srcKey = TopicKey.of(CommonUtils.randomString(10), CommonUtils.randomString(10))
    val targetKey = TopicKey.of(CommonUtils.randomString(10), CommonUtils.randomString(10))
    //test deleted topic
    createTopic(srcKey, false)
    createTopic(targetKey, false)
    testSourceConnector2Consumer(srcKey, targetKey)

    val srcKey2 = TopicKey.of(CommonUtils.randomString(10), CommonUtils.randomString(10))
    val targetKey2 = TopicKey.of(CommonUtils.randomString(10), CommonUtils.randomString(10))
    //test compacted topic
    createTopic(srcKey2, true)
    createTopic(targetKey2, true)
    testSourceConnector2Consumer(srcKey2, targetKey2)
  }

  /**
    * producer -> topic_1(topicName) -> row source -> topic_2 -> consumer
    */
  private[this] def testSourceConnector2Consumer(srcKey: TopicKey, targetKey: TopicKey): Unit = {
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(10), CommonUtils.randomString(10))
    result(
      workerClient
        .connectorCreator()
        .connectorKey(connectorKey)
        .connectorClass(classOf[SimpleRowSourceConnector])
        .topicKey(targetKey)
        .numberOfTasks(1)
        .columns(schema)
        .settings(Map(BROKER -> testUtil.brokersConnProps, INPUT -> srcKey.topicNameOnKafka))
        .create())

    try {
      checkConnector(connectorKey)
      setupData(srcKey)
      checkData(targetKey)
    } finally result(workerClient.delete(connectorKey))
  }

  /**
    * Test case for OHARA-150
    */
  @Test
  def shouldKeepColumnOrderAfterSendToKafka(): Unit = {
    val topicName = CommonUtils.randomString(10)
    val topicAdmin = TopicAdmin(testUtil().brokersConnProps())
    try topicAdmin.creator
      .topicKey(TopicKey.of("fake", topicName))
      .numberOfPartitions(1)
      .numberOfReplications(1)
      .create()
    finally topicAdmin.close()

    val row = Row.of(Cell.of("c", 3), Cell.of("b", 2), Cell.of("a", 1))
    val producer = Producer
      .builder[Row, Array[Byte]]()
      .connectionProps(testUtil.brokersConnProps)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    try {
      producer.sender().key(row).topicName(topicName).send()
      producer.flush()
    } finally producer.close()

    val consumer =
      Consumer
        .builder[Row, Array[Byte]]()
        .connectionProps(testUtil.brokersConnProps)
        .offsetFromBegin()
        .topicName(topicName)
        .keySerializer(Serializer.ROW)
        .valueSerializer(Serializer.BYTES)
        .build()

    try {
      val fromKafka = consumer.poll(java.time.Duration.ofSeconds(5), 1).asScala
      fromKafka.isEmpty shouldBe false
      val row = fromKafka.head.key.get
      row.cell(0).name shouldBe "c"
      row.cell(1).name shouldBe "b"
      row.cell(2).name shouldBe "a"

    } finally consumer.close()
  }

  /**
    * Test for ConnectorClient
    * @see ConnectorClient
    */
  @Test
  def testWorkerClient(): Unit = {
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(10), CommonUtils.randomString(10))
    val topicKeys = Set(TopicKey.of(CommonUtils.randomString(10), CommonUtils.randomString(10)))
    val outputTopic = CommonUtils.randomString(10)
    result(
      workerClient
        .connectorCreator()
        .connectorKey(connectorKey)
        .connectorClass(classOf[SimpleRowSinkConnector])
        .topicKeys(topicKeys)
        .numberOfTasks(1)
        .columns(schema)
        .settings(Map(BROKER -> testUtil.brokersConnProps, OUTPUT -> outputTopic))
        .create())

    val activeConnectors = result(workerClient.activeConnectors())
    activeConnectors.contains(connectorKey.connectorNameOnKafka()) shouldBe true

    val config = result(workerClient.config(connectorKey))
    config.topicNames shouldBe topicKeys.map(_.topicNameOnKafka)

    await(
      () =>
        try result(workerClient.status(connectorKey)).tasks.nonEmpty
        catch {
          case _: Throwable => false
      })
    val status = result(workerClient.status(connectorKey))
    status.tasks.head should not be null

    val task = result(workerClient.taskStatus(connectorKey, status.tasks.head.id))
    task should not be null
    task == status.tasks.head shouldBe true
    task.worker_id.isEmpty shouldBe false

    result(workerClient.delete(connectorKey))
    result(workerClient.activeConnectors()).contains(connectorKey.connectorNameOnKafka()) shouldBe false
  }
}
