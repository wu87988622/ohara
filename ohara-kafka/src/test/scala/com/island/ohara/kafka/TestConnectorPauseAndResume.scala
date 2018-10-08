package com.island.ohara.kafka

import com.island.ohara.client.ConnectorJson.State
import com.island.ohara.data.{Cell, Row}
import com.island.ohara.integration.{OharaTestUtil, With3Brokers3Workers}
import com.island.ohara.io.VersionUtil
import com.island.ohara.kafka.TestConnectorPauseAndResume._
import com.island.ohara.kafka.connector.{RowSourceConnector, RowSourceRecord, RowSourceTask, TaskConfig}
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestConnectorPauseAndResume extends With3Brokers3Workers with Matchers {

  @Test
  def testPauseAndResumeSource(): Unit = {
    val connectorName = methodName
    testUtil.connectorClient
      .connectorCreator()
      .topic(TOPIC)
      .connectorClass(classOf[MyConnector])
      .name(connectorName)
      .numberOfTasks(1)
      .disableConverter()
      .create()

    try {
      OharaTestUtil
        .await(() => testUtil.connectorClient.status(connectorName).connector.state == State.RUNNING, 50 seconds)
      val consumer =
        Consumer.builder().topicName(TOPIC).offsetFromBegin().brokers(testUtil.brokers).build[Array[Byte], Row]
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
                            2000)

        // since connector is resumed so some data are generated
        result = consumer.poll(20 seconds, 1)
        result.size should not be 0
      } finally consumer.close()
    } finally testUtil.connectorClient.delete(connectorName)
  }
}

private object TestConnectorPauseAndResume {
  val ROW = Row(Cell("f0", 13), Cell("f1", false))
  val TOPIC = "TestConnectorPause"
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

  override protected def _start(config: TaskConfig): Unit = {}

  override protected def _stop(): Unit = {}

  override protected def _poll(): Seq[RowSourceRecord] = {
    val current = System.currentTimeMillis()
    if (current - lastSent >= 1000) {
      lastSent = current
      Seq(RowSourceRecord(TestConnectorPauseAndResume.TOPIC, TestConnectorPauseAndResume.ROW))
    } else Seq.empty
  }

  override protected def _version: String = VersionUtil.VERSION
}
