package com.island.ohara.configurator

import java.time.Duration

import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.{ConfiguratorClient, ConnectorClient}
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.data.connector.State
import com.island.ohara.common.util.{CommonUtil, ReleaseOnce}
import com.island.ohara.configurator.store.Store
import com.island.ohara.integration.WithBrokerWorker
import com.island.ohara.kafka.KafkaClient
import com.island.ohara.kafka.connector.{RowSinkConnector, RowSinkRecord, RowSinkTask, TaskConfig}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._

class TestControlSink extends WithBrokerWorker with Matchers {

  private[this] val configurator = {
    val topicName = random()
    Configurator
      .builder()
      .hostname("localhost")
      .port(0)
      .store(
        Store
          .builder()
          .topicName(topicName)
          .brokers(testUtil.brokersConnProps)
          .build(Serializer.STRING, Serializer.OBJECT))
      .kafkaClient(KafkaClient.of(testUtil.brokersConnProps))
      .connectClient(ConnectorClient(testUtil.workersConnProps))
      .build()
  }

  private[this] val client = ConfiguratorClient(configurator.hostname, configurator.port)

  @Test
  def testNormalCase(): Unit = {
    val topicName = methodName
    val topic = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(topicName, 1, 1))
    val request = SinkRequest(name = methodName,
                              className = classOf[DumbSink].getName,
                              schema = Seq.empty,
                              topics = Seq(topic.uuid),
                              numberOfTasks = 1,
                              configs = Map.empty)

    val sink = client.add[SinkRequest, Sink](request)

    // test idempotent start
    (0 until 3).foreach(_ => client.start[Sink](sink.uuid))
    val connectorClient = ConnectorClient(testUtil.workersConnProps)
    try {

      CommonUtil.await(() =>
                         try connectorClient.exist(sink.uuid)
                         catch {
                           case _: Throwable => false
                       },
                       Duration.ofSeconds(30))
      CommonUtil.await(() => connectorClient.status(sink.uuid).connector.state == State.RUNNING, Duration.ofSeconds(20))
      client.get[Sink](sink.uuid).state.get shouldBe State.RUNNING

      // test idempotent pause
      (0 until 3).foreach(_ => client.pause[Sink](sink.uuid))
      CommonUtil.await(() => connectorClient.status(sink.uuid).connector.state == State.PAUSED, Duration.ofSeconds(20))
      client.get[Sink](sink.uuid).state.get shouldBe State.PAUSED

      // test idempotent resume
      (0 until 3).foreach(_ => client.resume[Sink](sink.uuid))
      CommonUtil.await(() => connectorClient.status(sink.uuid).connector.state == State.RUNNING, Duration.ofSeconds(20))
      client.get[Sink](sink.uuid).state.get shouldBe State.RUNNING

      // test idempotent stop. the connector should be removed
      (0 until 3).foreach(_ => client.stop[Sink](sink.uuid))
      CommonUtil.await(() => connectorClient.nonExist(sink.uuid), Duration.ofSeconds(20))
      client.get[Sink](sink.uuid).state shouldBe None
    } finally {
      if (connectorClient.exist(sink.uuid)) connectorClient.delete(sink.uuid)
      connectorClient.close()
    }
  }

  @Test
  def testUpdateRunningSink(): Unit = {
    val topicName = methodName
    val topic = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(topicName, 1, 1))
    val request = SinkRequest(name = methodName,
                              className = classOf[DumbSink].getName,
                              schema = Seq.empty,
                              topics = Seq(topic.uuid),
                              numberOfTasks = 1,
                              configs = Map.empty)

    val sink = client.add[SinkRequest, Sink](request)
    // test start
    client.start[Sink](sink.uuid)
    val connectorClient = ConnectorClient(testUtil.workersConnProps)
    try {
      CommonUtil.await(() =>
                         try connectorClient.exist(sink.uuid)
                         catch {
                           case _: Throwable => false
                       },
                       Duration.ofSeconds(30))
      CommonUtil.await(() => connectorClient.status(sink.uuid).connector.state == State.RUNNING, Duration.ofSeconds(20))

      an[IllegalArgumentException] should be thrownBy client
        .update[SinkRequest, Sink](sink.uuid, request.copy(numberOfTasks = 2))
      an[IllegalArgumentException] should be thrownBy client.delete[Sink](sink.uuid)

      // test stop. the connector should be removed
      client.stop[Sink](sink.uuid)
      CommonUtil.await(() => connectorClient.nonExist(sink.uuid), Duration.ofSeconds(20))
      client.get[Sink](sink.uuid).state shouldBe None
    } finally {
      if (connectorClient.exist(sink.uuid)) connectorClient.delete(sink.uuid)
      connectorClient.close()
    }
  }

  @Test
  def testCommandMsg(): Unit = {
    val topicName = methodName
    val request = SinkRequest(name = methodName,
                              className = classOf[DumbSink].getName,
                              schema = Seq.empty,
                              topics = Seq(topicName),
                              numberOfTasks = 1,
                              configs = Map.empty)

    val fakeUUID = random()
    (the[IllegalArgumentException] thrownBy client.pause[Sink](fakeUUID)).getMessage should include("exist")
    (the[IllegalArgumentException] thrownBy client.resume[Sink](fakeUUID)).getMessage should include("exist")

    val sink = client.add[SinkRequest, Sink](request)

    (the[IllegalArgumentException] thrownBy client.pause[Sink](sink.uuid)).getMessage should include("start")
    (the[IllegalArgumentException] thrownBy client.resume[Sink](sink.uuid)).getMessage should include("start")
  }
  @After
  def tearDown(): Unit = {
    ReleaseOnce.close(client)
    ReleaseOnce.close(configurator)
  }
}

class DumbSink extends RowSinkConnector {
  private[this] var config: TaskConfig = _
  override protected def _taskClass(): Class[_ <: RowSinkTask] = classOf[DumbSinkTask]
  override protected def _taskConfigs(maxTasks: Int): java.util.List[TaskConfig] = Seq.fill(maxTasks)(config).asJava
  override protected def _start(config: TaskConfig): Unit = {
    this.config = config
  }
  override protected def _stop(): Unit = {}
}

class DumbSinkTask extends RowSinkTask {
  override protected def _start(config: TaskConfig): Unit = {}

  override protected def _stop(): Unit = {}

  override protected def _put(records: java.util.List[RowSinkRecord]): Unit = {}
}
