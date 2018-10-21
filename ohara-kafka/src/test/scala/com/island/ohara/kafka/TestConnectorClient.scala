package com.island.ohara.kafka

import com.island.ohara.client.ConnectorJson.State
import com.island.ohara.data.{Cell, Row}
import com.island.ohara.integration.{OharaTestUtil, With3Brokers3Workers}
import com.island.ohara.io.VersionUtil
import com.island.ohara.kafka.TestConnectorClient._
import com.island.ohara.kafka.connector.{RowSourceConnector, RowSourceRecord, RowSourceTask, TaskConfig}
import org.junit.{Ignore, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestConnectorClient extends With3Brokers3Workers with Matchers {

//  @Ignore
  @Test
  def testExist(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    testUtil.connectorClient.exist(connectorName) shouldBe false

    testUtil.connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[MyConnector])
      .name(connectorName)
      .numberOfTasks(1)
      .disableConverter()
      .create()

    try {
      OharaTestUtil.await(() => testUtil.connectorClient.exist(connectorName), 50 seconds)
    } finally testUtil.connectorClient.delete(connectorName)
  }

  @Test
  def testExistOnUnrunnableConnector(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    testUtil.connectorClient.exist(connectorName) shouldBe false

    testUtil.connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[UnrunnableConnector])
      .name(connectorName)
      .numberOfTasks(1)
      .disableConverter()
      .create()

    try {
      OharaTestUtil.await(() => testUtil.connectorClient.exist(connectorName), 50 seconds)
    } finally testUtil.connectorClient.delete(connectorName)
  }

  @Test
  def testPauseAndResumeSource(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    testUtil.connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[MyConnector])
      .name(connectorName)
      .numberOfTasks(1)
      .disableConverter()
      .create()
    try {
      OharaTestUtil.await(() => testUtil.connectorClient.exist(connectorName), 50 seconds)
      val consumer =
        Consumer
          .builder()
          .topicName(topicName)
          .offsetFromBegin()
          .brokers(testUtil.brokersConnProps)
          .build[Array[Byte], Row]
      try {
        // try to receive some data from topic
        var result = consumer.poll(10 seconds, 1)
        result.size should not be 0
        result.foreach(_.value.get shouldBe ROW)

        // pause connector
        testUtil.connectorClient.pause(connectorName)
        OharaTestUtil
          .await(() => testUtil.connectorClient.status(connectorName).connector.state == State.PAUSED, 50 seconds)

        // try to receive all data from topic...10 seconds should be enough in this case
        result = consumer.poll(10 seconds, Int.MaxValue)
        result.foreach(_.value.get shouldBe ROW)

        // connector is paused so there is no data
        result = consumer.poll(20 seconds, 1)
        result.size shouldBe 0

        // resume connector
        testUtil.connectorClient.resume(connectorName)

        OharaTestUtil.await(() => testUtil.connectorClient.status(connectorName).connector.state == State.RUNNING,
                            50 seconds,
                            2 seconds)

        // since connector is resumed so some data are generated
        result = consumer.poll(20 seconds, 1)
        result.size should not be 0
      } finally consumer.close()
    } finally testUtil.connectorClient.delete(connectorName)
  }
}

private object TestConnectorClient {
  val ROW = Row(Cell("f0", 13), Cell("f1", false))
}

class MyConnector extends RowSourceConnector {
  private[this] var config: TaskConfig = _
  override protected def _taskClass(): Class[_ <: RowSourceTask] = classOf[MyConnectorTask]

  override protected def _taskConfigs(maxTasks: Int): Seq[TaskConfig] = Seq.fill(maxTasks)(config)

  override protected def _start(config: TaskConfig): Unit = {
    this.config = config
  }

  override protected def _stop(): Unit = {}

  override protected def _version: String = VersionUtil.VERSION
}

class MyConnectorTask extends RowSourceTask {
  private[this] var lastSent: Long = 0
  private[this] var topicName: String = _

  override protected def _start(config: TaskConfig): Unit = {
    this.topicName = config.topics.head
  }

  override protected def _stop(): Unit = {}

  override protected def _poll(): Seq[RowSourceRecord] = {
    val current = System.currentTimeMillis()
    if (current - lastSent >= 1000) {
      lastSent = current
      Seq(RowSourceRecord(topicName, TestConnectorClient.ROW))
    } else Seq.empty
  }

  override protected def _version: String = VersionUtil.VERSION
}

class UnrunnableConnector extends RowSourceConnector {
  override protected def _taskClass(): Class[_ <: RowSourceTask] = classOf[UnrunnableConnectorTask]

  override protected def _taskConfigs(maxTasks: Int): Seq[TaskConfig] = throw new IllegalArgumentException(
    "This is an unrunnable connector")

  override protected def _start(config: TaskConfig): Unit = {
    throw new IllegalArgumentException("This is an unrunnable connector")
  }

  override protected def _stop(): Unit = {}

  override protected def _version: String = VersionUtil.VERSION
}

class UnrunnableConnectorTask extends RowSourceTask {
  override protected def _start(config: TaskConfig): Unit = {}

  override protected def _stop(): Unit = {}

  override protected def _poll(): Seq[RowSourceRecord] = Seq.empty

  override protected def _version: String = VersionUtil.VERSION
}
