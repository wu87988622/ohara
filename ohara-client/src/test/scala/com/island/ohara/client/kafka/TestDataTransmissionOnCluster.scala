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

import com.island.ohara.common.data._
import com.island.ohara.common.util.{CommonUtil, Releasable}
import com.island.ohara.kafka.{BrokerClient, Consumer, Producer}
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._
class TestDataTransmissionOnCluster extends With3Brokers3Workers with Matchers {

  private[this] val brokerClient = BrokerClient.of(testUtil().brokersConnProps)
  private[this] val workerClient = WorkerClient(testUtil().workersConnProps())
  private[this] val row = Row.of(Cell.of("cf0", 10), Cell.of("cf1", 11))
  private[this] val schema = Seq(Column.of("cf", DataType.BOOLEAN, 1))
  private[this] val numberOfRows = 20

  @After
  def tearDown(): Unit = Releasable.close(brokerClient)

  private[this] def createTopic(topicName: String, compacted: Boolean): Unit = {
    if (compacted)
      brokerClient.topicCreator().compacted().numberOfPartitions(1).numberOfReplications(1).create(topicName)
    else
      brokerClient.topicCreator().deleted().numberOfPartitions(1).numberOfReplications(1).create(topicName)
  }

  private[this] def setupData(topicName: String): Unit = {
    val producer = Producer.builder().connectionProps(testUtil.brokersConnProps).build(Serializer.ROW, Serializer.BYTES)
    try 0 until numberOfRows foreach (_ => producer.sender().key(row).send(topicName))
    finally producer.close()
    checkData(topicName)
  }

  private[this] def checkData(topicName: String): Unit = {
    val consumer = Consumer
      .builder()
      .offsetFromBegin()
      .connectionProps(testUtil.brokersConnProps)
      .topicName(topicName)
      .build(Serializer.ROW, Serializer.BYTES)
    val data = consumer.poll(java.time.Duration.ofSeconds(30), numberOfRows)
    data.size shouldBe numberOfRows
    data.asScala.foreach(_.key.get shouldBe row)
  }

  private[this] def checkConnector(name: String): Unit = {
    await(() => result(workerClient.activeConnectors()).contains(name))
    await(() => result(workerClient.config(name)).topics.nonEmpty)
    await(
      () =>
        try result(workerClient.status(name)).connector.state == ConnectorState.RUNNING
        catch {
          case _: Throwable => false
      })
  }

  @Test
  def testRowProducer2RowConsumer(): Unit = {
    var topicName = methodName
    //test deleted topic
    createTopic(topicName, false)
    testRowProducer2RowConsumer(topicName)

    topicName = methodName + "-2"
    //test compacted topic
    createTopic(topicName, true)
    testRowProducer2RowConsumer(topicName)
  }

  /**
    * producer -> topic_1(topicName) -> consumer
    */
  private[this] def testRowProducer2RowConsumer(topicName: String): Unit = {
    setupData(topicName)
    val consumer = Consumer
      .builder()
      .offsetFromBegin()
      .connectionProps(testUtil.brokersConnProps)
      .topicName(topicName)
      .build(Serializer.ROW, Serializer.BYTES)
    try {
      val data = consumer.poll(java.time.Duration.ofSeconds(30), numberOfRows)
      data.size shouldBe numberOfRows
      data.asScala.foreach(_.key.get shouldBe row)
    } finally consumer.close()
  }

  @Test
  def testProducer2SinkConnector(): Unit = {
    val topicName = CommonUtil.randomString(10)
    val topicName2 = CommonUtil.randomString(10)
    //test deleted topic
    createTopic(topicName, false)
    createTopic(topicName2, false)
    testProducer2SinkConnector(topicName, topicName2)

    val topicName3 = CommonUtil.randomString(10)
    val topicName4 = CommonUtil.randomString(10)
    //test compacted topic
    createTopic(topicName3, true)
    createTopic(topicName4, true)
    testProducer2SinkConnector(topicName3, topicName4)
  }

  /**
    * producer -> topic_1(topicName) -> sink connector -> topic_2(topicName2)
    */
  private[this] def testProducer2SinkConnector(topicName: String, topicName2: String): Unit = {
    val connectorName = CommonUtil.randomString(10)
    result(
      workerClient
        .connectorCreator()
        .name(connectorName)
        .connectorClass(classOf[SimpleRowSinkConnector])
        .topic(topicName)
        .numberOfTasks(2)
        .disableConverter()
        .schema(schema)
        .configs(Map(BROKER -> testUtil.brokersConnProps, OUTPUT -> topicName2))
        .create())

    try {
      checkConnector(connectorName)
      setupData(topicName)
      checkData(topicName2)
    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testSourceConnector2Consumer(): Unit = {
    var topicName = methodName
    var topicName2 = methodName + "-2"
    //test deleted topic
    createTopic(topicName, false)
    createTopic(topicName2, false)
    testSourceConnector2Consumer(topicName, topicName2)

    topicName = methodName + "-3"
    topicName2 = methodName + "-4"
    //test compacted topic
    createTopic(topicName, true)
    createTopic(topicName2, true)
    testSourceConnector2Consumer(topicName, topicName2)
  }

  /**
    * producer -> topic_1(topicName) -> row source -> topic_2 -> consumer
    */
  private[this] def testSourceConnector2Consumer(topicName: String, topicName2: String): Unit = {
    val connectorName = CommonUtil.randomString(10)
    result(
      workerClient
        .connectorCreator()
        .name(connectorName)
        .connectorClass(classOf[SimpleRowSourceConnector])
        .topic(topicName2)
        .numberOfTasks(2)
        .disableConverter()
        .schema(schema)
        .configs(Map(BROKER -> testUtil.brokersConnProps, INPUT -> topicName))
        .create())

    try {
      checkConnector(connectorName)
      setupData(topicName)
      checkData(topicName2)
    } finally result(workerClient.delete(connectorName))
  }

  /**
    * Test case for OHARA-150
    */
  @Test
  def shouldKeepColumnOrderAfterSendToKafka(): Unit = {
    val topicName = CommonUtil.randomString(10)
    val topicAdmin = TopicAdmin(testUtil().brokersConnProps())
    try topicAdmin.creator().name(topicName).numberOfPartitions(1).numberOfReplications(1).create()
    finally topicAdmin.close()

    val row = Row.of(Cell.of("c", 3), Cell.of("b", 2), Cell.of("a", 1))
    val producer = Producer.builder().connectionProps(testUtil.brokersConnProps).build(Serializer.ROW, Serializer.BYTES)
    try {
      producer.sender().key(row).send(topicName)
      producer.flush()
    } finally producer.close()

    val consumer =
      Consumer
        .builder()
        .connectionProps(testUtil.brokersConnProps)
        .offsetFromBegin()
        .topicName(topicName)
        .build(Serializer.ROW, Serializer.BYTES)

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
  def connectorClientTest(): Unit = {
    val connectorName = CommonUtil.randomString(10)
    val topics = Seq(CommonUtil.randomString(10), CommonUtil.randomString(10))
    val outputTopic = CommonUtil.randomString(10)
    result(
      workerClient
        .connectorCreator()
        .name(connectorName)
        .connectorClass(classOf[SimpleRowSinkConnector])
        .topics(topics)
        .numberOfTasks(2)
        .disableConverter()
        .schema(schema)
        .configs(Map(BROKER -> testUtil.brokersConnProps, OUTPUT -> outputTopic))
        .create())

    val activeConnectors = result(workerClient.activeConnectors())
    activeConnectors.contains(connectorName) shouldBe true

    val config = result(workerClient.config(connectorName))
    config.topics shouldBe topics

    await(() => result(workerClient.status(connectorName)).tasks.nonEmpty)
    val status = result(workerClient.status(connectorName))
    status.tasks.head should not be null

    val task = result(workerClient.taskStatus(connectorName, status.tasks.head.id))
    task should not be null
    task == status.tasks.head shouldBe true
    task.worker_id.isEmpty shouldBe false

    result(workerClient.delete(connectorName))
    result(workerClient.activeConnectors()).contains(connectorName) shouldBe false
  }
}
