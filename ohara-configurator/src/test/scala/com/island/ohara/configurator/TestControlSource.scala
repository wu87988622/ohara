package com.island.ohara.configurator

import java.time.Duration

import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.{ConfiguratorClient, ConnectorClient}
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.data.connector.ConnectorState
import com.island.ohara.common.util.{CommonUtil, ReleaseOnce}
import com.island.ohara.configurator.store.Store
import com.island.ohara.integration.WithBrokerWorker
import com.island.ohara.kafka.KafkaClient
import com.island.ohara.kafka.connector.{RowSourceConnector, RowSourceRecord, RowSourceTask, TaskConfig}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._

class TestControlSource extends WithBrokerWorker with Matchers {

  private[this] val configurator = Configurator
    .builder()
    .hostname("localhost")
    .port(0)
    .store(
      Store
        .builder()
        .topicName(random())
        .brokers(testUtil.brokersConnProps)
        .build(Serializer.STRING, Serializer.OBJECT))
    .kafkaClient(KafkaClient.of(testUtil.brokersConnProps))
    .connectClient(ConnectorClient(testUtil.workersConnProps))
    .build()

  private[this] val client = ConfiguratorClient(configurator.hostname, configurator.port)

  @Test
  def testNormalCase(): Unit = {
    val topicName = methodName
    val topic = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(topicName, 1, 1))
    val request = SourceRequest(name = methodName,
                                className = classOf[DumbSource].getName,
                                schema = Seq.empty,
                                topics = Seq(topic.id),
                                numberOfTasks = 1,
                                configs = Map.empty)

    val source = client.add[SourceRequest, Source](request)

    // test idempotent start
    (0 until 3).foreach(_ => client.start[Source](source.id))
    val connectorClient = ConnectorClient(testUtil.workersConnProps)
    try {
      CommonUtil.await(() =>
                         try connectorClient.exist(source.id)
                         catch {
                           case _: Throwable => false
                       },
                       Duration.ofSeconds(30))
      CommonUtil.await(() => connectorClient.status(source.id).connector.state == ConnectorState.RUNNING,
                       Duration.ofSeconds(20))
      client.get[Source](source.id).state.get shouldBe ConnectorState.RUNNING

      // test idempotent pause
      (0 until 3).foreach(_ => client.pause[Source](source.id))

      CommonUtil
        .await(() => connectorClient.status(source.id).connector.state == ConnectorState.PAUSED, Duration.ofSeconds(20))
      client.get[Source](source.id).state.get shouldBe ConnectorState.PAUSED

      // test idempotent resume
      (0 until 3).foreach(_ => client.resume[Source](source.id))
      CommonUtil.await(() => connectorClient.status(source.id).connector.state == ConnectorState.RUNNING,
                       Duration.ofSeconds(20))
      client.get[Source](source.id).state.get shouldBe ConnectorState.RUNNING

      // test idempotent stop. the connector should be removed
      (0 until 3).foreach(_ => client.stop[Source](source.id))

      CommonUtil.await(() => connectorClient.nonExist(source.id), Duration.ofSeconds(20))
      client.get[Source](source.id).state shouldBe None
    } finally {
      if (connectorClient.exist(source.id)) connectorClient.delete(source.id)
      ReleaseOnce.close(connectorClient)
    }
  }

  @Test
  def testUpdateRunningSource(): Unit = {
    val topicName = methodName
    val topic = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(topicName, 1, 1))
    val request = SourceRequest(name = methodName,
                                className = classOf[DumbSource].getName,
                                schema = Seq.empty,
                                topics = Seq(topic.id),
                                numberOfTasks = 1,
                                configs = Map.empty)

    val source = client.add[SourceRequest, Source](request)
    // test start
    client.start[Source](source.id)
    val connectorClient = ConnectorClient(testUtil.workersConnProps)
    try {
      CommonUtil.await(() =>
                         try connectorClient.exist(source.id)
                         catch {
                           case _: Throwable => false
                       },
                       Duration.ofSeconds(30))
      CommonUtil.await(() => connectorClient.status(source.id).connector.state == ConnectorState.RUNNING,
                       Duration.ofSeconds(20))

      an[IllegalArgumentException] should be thrownBy client
        .update[SourceRequest, Source](source.id, request.copy(numberOfTasks = 2))
      an[IllegalArgumentException] should be thrownBy client.delete[Source](source.id)

      // test stop. the connector should be removed
      client.stop[Source](source.id)
      CommonUtil.await(() => connectorClient.nonExist(source.id), Duration.ofSeconds(20))
      client.get[Source](source.id).state shouldBe None
    } finally {
      if (connectorClient.exist(source.id)) connectorClient.delete(source.id)
      ReleaseOnce.close(connectorClient)
    }
  }

  @Test
  def testCommandMsg(): Unit = {
    val topicName = methodName
    val request = SourceRequest(name = methodName,
                                className = classOf[DumbSource].getName,
                                schema = Seq.empty,
                                topics = Seq(topicName),
                                numberOfTasks = 1,
                                configs = Map.empty)

    val fakeUUID = random()
    (the[IllegalArgumentException] thrownBy client.pause[Source](fakeUUID)).getMessage should include("exist")
    (the[IllegalArgumentException] thrownBy client.resume[Source](fakeUUID)).getMessage should include("exist")

    val source = client.add[SourceRequest, Source](request)

    (the[IllegalArgumentException] thrownBy client.pause[Source](source.id)).getMessage should include("start")
    (the[IllegalArgumentException] thrownBy client.resume[Source](source.id)).getMessage should include("start")
  }

  @After
  def tearDown(): Unit = {
    ReleaseOnce.close(client)
    ReleaseOnce.close(configurator)
  }
}

class DumbSource extends RowSourceConnector {
  private[this] var config: TaskConfig = _
  override protected def _taskClass(): Class[_ <: RowSourceTask] = classOf[DumbSourceTask]
  override protected def _taskConfigs(maxTasks: Int): java.util.List[TaskConfig] = Seq.fill(maxTasks)(config).asJava
  override protected def _start(config: TaskConfig): Unit = {
    this.config = config
  }
  override protected def _stop(): Unit = {}
}

class DumbSourceTask extends RowSourceTask {
  override protected def _start(config: TaskConfig): Unit = {}

  override protected def _stop(): Unit = {}

  override protected def _poll(): java.util.List[RowSourceRecord] = Seq.empty.asJava
}
