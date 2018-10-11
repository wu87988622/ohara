package com.island.ohara.configurator

import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.ConnectorJson.State
import com.island.ohara.client.{ConfiguratorClient, ConnectorClient}
import com.island.ohara.configurator.store.Store
import com.island.ohara.integration.{OharaTestUtil, With3Brokers3Workers}
import com.island.ohara.io.CloseOnce.doClose
import com.island.ohara.io.{CloseOnce, VersionUtil}
import com.island.ohara.kafka.KafkaClient
import com.island.ohara.kafka.connector.{RowSourceConnector, RowSourceRecord, RowSourceTask, TaskConfig}
import com.island.ohara.serialization.DataType
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
class TestControlSource extends With3Brokers3Workers with Matchers {

  private[this] val configurator = {
    val topicName = random()
    doClose(KafkaClient(testUtil.brokers))(
      _.topicCreator().numberOfPartitions(1).numberOfReplications(1).compacted().create(topicName))
    Configurator
      .builder()
      .hostname("localhost")
      .port(0)
      .store(Store.builder().topicName(topicName).brokers(testUtil.brokers).buildBlocking[String, Any])
      .kafkaClient(KafkaClient(testUtil.brokers))
      .connectClient(ConnectorClient(testUtil.workers))
      .build()
  }

  private[this] val client = ConfiguratorClient(configurator.hostname, configurator.port)

  @Test
  def testNormalCase(): Unit = {
    val topicName = methodName
    val request = SourceRequest(name = methodName,
                                className = classOf[DumbSource].getName,
                                schema = Seq.empty,
                                topics = Seq(topicName),
                                numberOfTasks = 1,
                                configs = Map.empty)

    val source = client.add[SourceRequest, Source](request)

    // test start
    client.start[Source](source.uuid)
    try {
      OharaTestUtil.await(() => testUtil.connectorClient.exist(source.uuid), 30 seconds)
      OharaTestUtil
        .await(() => testUtil.connectorClient.status(source.uuid).connector.state == State.RUNNING, 20 seconds)
      client.get[Source](source.uuid).state.get shouldBe State.RUNNING

      // test pause
      client.pause[Source](source.uuid)
      OharaTestUtil
        .await(() => testUtil.connectorClient.status(source.uuid).connector.state == State.PAUSED, 20 seconds)
      client.get[Source](source.uuid).state.get shouldBe State.PAUSED

      // test resume
      client.resume[Source](source.uuid)
      OharaTestUtil
        .await(() => testUtil.connectorClient.status(source.uuid).connector.state == State.RUNNING, 20 seconds)
      client.get[Source](source.uuid).state.get shouldBe State.RUNNING

      // test stop. the connector should be removed
      client.stop[Source](source.uuid)
      OharaTestUtil.await(() => testUtil.connectorClient.nonExist(source.uuid), 20 seconds)
      client.get[Source](source.uuid).state shouldBe None
    } finally if (testUtil.connectorClient.exist(source.uuid)) testUtil.connectorClient.delete(source.uuid)
  }

  @Test
  def testUpdateRunningSource(): Unit = {
    val topicName = methodName
    val request = SourceRequest(name = methodName,
                                className = classOf[DumbSource].getName,
                                schema = Seq.empty,
                                topics = Seq(topicName),
                                numberOfTasks = 1,
                                configs = Map.empty)

    val source = client.add[SourceRequest, Source](request)
    // test start
    client.start[Source](source.uuid)
    try {
      OharaTestUtil.await(() => testUtil.connectorClient.exist(source.uuid), 30 seconds)
      OharaTestUtil
        .await(() => testUtil.connectorClient.status(source.uuid).connector.state == State.RUNNING, 20 seconds)

      an[IllegalArgumentException] should be thrownBy client
        .update[SourceRequest, Source](source.uuid, request.copy(numberOfTasks = 2))
      an[IllegalArgumentException] should be thrownBy client.delete[Source](source.uuid)

      // test stop. the connector should be removed
      client.stop[Source](source.uuid)
      OharaTestUtil.await(() => testUtil.connectorClient.nonExist(source.uuid), 20 seconds)
      client.get[Source](source.uuid).state shouldBe None
    } finally if (testUtil.connectorClient.exist(source.uuid)) testUtil.connectorClient.delete(source.uuid)
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(client)
    CloseOnce.close(configurator)
  }
}

class DumbSource extends RowSourceConnector {
  private[this] var config: TaskConfig = _
  override protected def _taskClass(): Class[_ <: RowSourceTask] = classOf[DumbSourceTask]
  override protected def _taskConfigs(maxTasks: Int): Seq[TaskConfig] = Seq.fill(maxTasks)(config)
  override protected def _start(config: TaskConfig): Unit = {
    this.config = config
  }
  override protected def _stop(): Unit = {}

  override protected def _version: String = VersionUtil.VERSION
}

class DumbSourceTask extends RowSourceTask {
  override protected def _start(config: TaskConfig): Unit = {}

  override protected def _stop(): Unit = {}

  override protected def _poll(): Seq[RowSourceRecord] = Seq.empty

  override protected def _version: String = VersionUtil.VERSION
}
