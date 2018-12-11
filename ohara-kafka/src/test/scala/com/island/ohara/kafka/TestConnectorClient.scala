package com.island.ohara.kafka

import java.time.Duration
import java.util

import com.island.ohara.client.ConnectorClient
import com.island.ohara.client.ConnectorJson.State
import com.island.ohara.common.data.{Cell, Row, Serializer}
import com.island.ohara.common.util.{CloseOnce, CommonUtil}
import com.island.ohara.integration.{With3Brokers3Workers, WithBrokerWorker}
import com.island.ohara.kafka.connector.{RowSourceConnector, RowSourceRecord, RowSourceTask, TaskConfig}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._

class TestConnectorClient extends WithBrokerWorker with Matchers {
  private[this] val connectorClient = ConnectorClient(testUtil.workersConnProps)

  @After
  def tearDown(): Unit = CloseOnce.close(connectorClient)

  import TestConnectorClient.ROW
//  @Ignore
  @Test
  def testExist(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    connectorClient.exist(connectorName) shouldBe false

    connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[MyConnector])
      .name(connectorName)
      .numberOfTasks(1)
      .disableConverter()
      .create()

    try {
      CommonUtil.await(() => connectorClient.exist(connectorName), Duration.ofSeconds(50))
    } finally connectorClient.delete(connectorName)
  }

  @Test
  def testExistOnUnrunnableConnector(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    connectorClient.exist(connectorName) shouldBe false

    connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[UnrunnableConnector])
      .name(connectorName)
      .numberOfTasks(1)
      .disableConverter()
      .create()

    try {
      CommonUtil.await(() => connectorClient.exist(connectorName), Duration.ofSeconds(50))
    } finally connectorClient.delete(connectorName)
  }

  @Test
  def testPauseAndResumeSource(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[MyConnector])
      .name(connectorName)
      .numberOfTasks(1)
      .disableConverter()
      .create()
    try {
      CommonUtil.await(() => connectorClient.exist(connectorName), Duration.ofSeconds(50))
      val consumer =
        Consumer
          .builder()
          .topicName(topicName)
          .offsetFromBegin()
          .brokers(testUtil.brokersConnProps)
          .build(Serializer.BYTES, Serializer.ROW)
      try {
        // try to receive some data from topic
        var result = consumer.poll(java.time.Duration.ofSeconds(10), 1)
        result.size should not be 0
        result.asScala.foreach(_.value.get shouldBe ROW)

        // pause connector
        connectorClient.pause(connectorName)
        CommonUtil
          .await(() => connectorClient.status(connectorName).connector.state == State.PAUSED, Duration.ofSeconds(50))

        // try to receive all data from topic...10 seconds should be enough in this case
        result = consumer.poll(java.time.Duration.ofSeconds(10), Int.MaxValue)
        result.asScala.foreach(_.value.get shouldBe ROW)

        // connector is paused so there is no data
        result = consumer.poll(java.time.Duration.ofSeconds(20), 1)
        result.size shouldBe 0

        // resume connector
        connectorClient.resume(connectorName)

        CommonUtil.await(() => connectorClient.status(connectorName).connector.state == State.RUNNING,
                         Duration.ofSeconds(50),
                         Duration.ofSeconds(2),
                         true)

        // since connector is resumed so some data are generated
        result = consumer.poll(java.time.Duration.ofSeconds(20), 1)
        result.size should not be 0
      } finally consumer.close()
    } finally connectorClient.delete(connectorName)
  }
}

private object TestConnectorClient {
  val ROW: Row = Row.of(Cell.of("f0", 13), Cell.of("f1", false))
}

class MyConnector extends RowSourceConnector {
  private[this] var config: TaskConfig = _
  override protected def _taskClass(): Class[_ <: RowSourceTask] = {
    classOf[MyConnectorTask]
  }

  override protected def _taskConfigs(maxTasks: Int): java.util.List[TaskConfig] = {
    Seq.fill(maxTasks)(config).asJava
  }

  override protected def _start(config: TaskConfig): Unit = {
    this.config = config
  }

  override protected def _stop(): Unit = {}
}

class MyConnectorTask extends RowSourceTask {
  private[this] var lastSent: Long = 0
  private[this] var topicName: String = _

  override protected def _start(config: TaskConfig) = {
    this.topicName = config.topics().get(0)
  }

  override protected def _stop(): Unit = {}

  override protected def _poll(): util.List[RowSourceRecord] = {
    val current = System.currentTimeMillis()
    if (current - lastSent >= 1000) {
      lastSent = current
      Seq(RowSourceRecord.of(topicName, TestConnectorClient.ROW)).asJava
    } else
      Seq.empty.asJava
  }
}

class UnrunnableConnector extends RowSourceConnector {
  override protected def _taskClass(): Class[_ <: RowSourceTask] = classOf[UnrunnableConnectorTask]

  override protected def _taskConfigs(maxTasks: Int): util.List[TaskConfig] = {
    throw new IllegalArgumentException("This is an unrunnable connector")
  }

  override protected def _start(config: TaskConfig): Unit = {
    throw new IllegalArgumentException("This is an unrunnable connector")
  }

  override protected def _stop(): Unit = {}
}

class UnrunnableConnectorTask extends RowSourceTask {
  override protected def _start(config: TaskConfig) = {}
  override protected def _stop(): Unit = {}
  override protected def _poll(): util.List[RowSourceRecord] = Seq.empty.asJava
}
