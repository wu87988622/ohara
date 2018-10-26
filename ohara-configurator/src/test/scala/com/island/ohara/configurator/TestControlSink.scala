package com.island.ohara.configurator

import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.ConnectorJson.State
import com.island.ohara.client.{ConfiguratorClient, ConnectorClient}
import com.island.ohara.configurator.store.Store
import com.island.ohara.integration.{OharaTestUtil, With3Brokers3Workers}
import com.island.ohara.io.CloseOnce.doClose
import com.island.ohara.io.CloseOnce
import com.island.ohara.kafka.KafkaClient
import com.island.ohara.kafka.connector.{RowSinkConnector, RowSinkRecord, RowSinkTask, TaskConfig}
import com.island.ohara.serialization.DataType
import com.island.ohara.util.VersionUtil
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
class TestControlSink extends With3Brokers3Workers with Matchers {

  private[this] val configurator = {
    val topicName = random()
    doClose(KafkaClient(testUtil.brokersConnProps))(
      _.topicCreator().numberOfPartitions(1).numberOfReplications(1).compacted().create(topicName))
    Configurator
      .builder()
      .hostname("localhost")
      .port(0)
      .store(Store.builder().topicName(topicName).brokers(testUtil.brokersConnProps).buildBlocking[String, Any])
      .kafkaClient(KafkaClient(testUtil.brokersConnProps))
      .connectClient(ConnectorClient(testUtil.workersConnProps))
      .build()
  }

  private[this] val client = ConfiguratorClient(configurator.hostname, configurator.port)

  @Test
  def testNormalCase(): Unit = {
    val topicName = methodName
    val request = SinkRequest(name = methodName,
                              className = classOf[DumbSink].getName,
                              schema = Seq.empty,
                              topics = Seq(topicName),
                              numberOfTasks = 1,
                              configs = Map.empty)

    val sink = client.add[SinkRequest, Sink](request)

    // test idempotent start
    (0 until 3).foreach(_ => client.start[Sink](sink.uuid))
    try {
      OharaTestUtil.await(() =>
                            try testUtil.connectorClient.exist(sink.uuid)
                            catch {
                              case _: Throwable => false
                          },
                          30 seconds)
      OharaTestUtil.await(() => testUtil.connectorClient.status(sink.uuid).connector.state == State.RUNNING, 20 seconds)
      client.get[Sink](sink.uuid).state.get shouldBe State.RUNNING

      // test idempotent pause
      (0 until 3).foreach(_ => client.pause[Sink](sink.uuid))
      OharaTestUtil.await(() => testUtil.connectorClient.status(sink.uuid).connector.state == State.PAUSED, 20 seconds)
      client.get[Sink](sink.uuid).state.get shouldBe State.PAUSED

      // test idempotent resume
      (0 until 3).foreach(_ => client.resume[Sink](sink.uuid))
      OharaTestUtil.await(() => testUtil.connectorClient.status(sink.uuid).connector.state == State.RUNNING, 20 seconds)
      client.get[Sink](sink.uuid).state.get shouldBe State.RUNNING

      // test idempotent stop. the connector should be removed
      (0 until 3).foreach(_ => client.stop[Sink](sink.uuid))
      OharaTestUtil.await(() => testUtil.connectorClient.nonExist(sink.uuid), 20 seconds)
      client.get[Sink](sink.uuid).state shouldBe None
    } finally if (testUtil.connectorClient.exist(sink.uuid)) testUtil.connectorClient.delete(sink.uuid)
  }

  @Test
  def testUpdateRunningSink(): Unit = {
    val topicName = methodName
    val request = SinkRequest(name = methodName,
                              className = classOf[DumbSink].getName,
                              schema = Seq.empty,
                              topics = Seq(topicName),
                              numberOfTasks = 1,
                              configs = Map.empty)

    val sink = client.add[SinkRequest, Sink](request)
    // test start
    client.start[Sink](sink.uuid)
    try {
      OharaTestUtil.await(() =>
                            try testUtil.connectorClient.exist(sink.uuid)
                            catch {
                              case _: Throwable => false
                          },
                          30 seconds)
      OharaTestUtil.await(() => testUtil.connectorClient.status(sink.uuid).connector.state == State.RUNNING, 20 seconds)

      an[IllegalArgumentException] should be thrownBy client
        .update[SinkRequest, Sink](sink.uuid, request.copy(numberOfTasks = 2))
      an[IllegalArgumentException] should be thrownBy client.delete[Sink](sink.uuid)

      // test stop. the connector should be removed
      client.stop[Sink](sink.uuid)
      OharaTestUtil.await(() => testUtil.connectorClient.nonExist(sink.uuid), 20 seconds)
      client.get[Sink](sink.uuid).state shouldBe None
    } finally if (testUtil.connectorClient.exist(sink.uuid)) testUtil.connectorClient.delete(sink.uuid)
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(client)
    CloseOnce.close(configurator)
  }
}

class DumbSink extends RowSinkConnector {
  private[this] var config: TaskConfig = _
  override protected def _taskClass(): Class[_ <: RowSinkTask] = classOf[DumbSinkTask]
  override protected def _taskConfigs(maxTasks: Int): Seq[TaskConfig] = Seq.fill(maxTasks)(config)
  override protected def _start(config: TaskConfig): Unit = {
    this.config = config
  }
  override protected def _stop(): Unit = {}

  override protected def _version: String = VersionUtil.VERSION
}

class DumbSinkTask extends RowSinkTask {
  override protected def _start(config: TaskConfig): Unit = {}

  override protected def _stop(): Unit = {}

  override protected def _version: String = VersionUtil.VERSION

  override protected def _put(records: Seq[RowSinkRecord]): Unit = {}
}
