package com.island.ohara.configurator

import java.time.Duration

import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.configurator.v0.ConnectorApi.{ConnectorConfiguration, ConnectorConfigurationRequest}
import com.island.ohara.client.configurator.v0.{ConnectorApi, TopicApi}
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.client.{ConfiguratorClient, ConnectorClient}
import com.island.ohara.common.data.ConnectorState
import com.island.ohara.common.util.{CommonUtil, ReleaseOnce}
import com.island.ohara.integration.WithBrokerWorker
import com.island.ohara.kafka.KafkaClient
import com.island.ohara.kafka.connector.{RowSinkConnector, RowSinkRecord, RowSinkTask, TaskConfig}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestControlSink extends WithBrokerWorker with Matchers {

  private[this] val configurator = Configurator
    .builder()
    .hostname("localhost")
    .port(0)
    .kafkaClient(KafkaClient.of(testUtil.brokersConnProps))
    .connectClient(ConnectorClient(testUtil.workersConnProps))
    .build()

  private[this] val client = ConfiguratorClient(configurator.hostname, configurator.port)
  private[this] val access = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)
  @Test
  def testNormalCase(): Unit = {
    val topicName = methodName
    val topic = Await.result(TopicApi
                               .access()
                               .hostname(configurator.hostname)
                               .port(configurator.port)
                               .add(TopicCreationRequest(topicName, 1, 1)),
                             10 seconds)
    val request = ConnectorConfigurationRequest(name = methodName,
                                                className = classOf[DumbSink].getName,
                                                schema = Seq.empty,
                                                topics = Seq(topic.id),
                                                numberOfTasks = 1,
                                                configs = Map.empty)

    val sink = result(access.add(request))

    // test idempotent start
    (0 until 3).foreach(_ => client.start[ConnectorConfiguration](sink.id))
    val connectorClient = ConnectorClient(testUtil.workersConnProps)
    try {

      CommonUtil.await(() =>
                         try connectorClient.exist(sink.id)
                         catch {
                           case _: Throwable => false
                       },
                       Duration.ofSeconds(30))
      CommonUtil
        .await(() => connectorClient.status(sink.id).connector.state == ConnectorState.RUNNING, Duration.ofSeconds(20))
      result(access.get(sink.id)).state.get shouldBe ConnectorState.RUNNING

      // test idempotent pause
      (0 until 3).foreach(_ => client.pause[ConnectorConfiguration](sink.id))
      CommonUtil
        .await(() => connectorClient.status(sink.id).connector.state == ConnectorState.PAUSED, Duration.ofSeconds(20))
      result(access.get(sink.id)).state.get shouldBe ConnectorState.PAUSED

      // test idempotent resume
      (0 until 3).foreach(_ => client.resume[ConnectorConfiguration](sink.id))
      CommonUtil
        .await(() => connectorClient.status(sink.id).connector.state == ConnectorState.RUNNING, Duration.ofSeconds(20))
      result(access.get(sink.id)).state.get shouldBe ConnectorState.RUNNING

      // test idempotent stop. the connector should be removed
      (0 until 3).foreach(_ => client.stop[ConnectorConfiguration](sink.id))
      CommonUtil.await(() => connectorClient.nonExist(sink.id), Duration.ofSeconds(20))
      result(access.get(sink.id)).state shouldBe None
    } finally {
      if (connectorClient.exist(sink.id)) connectorClient.delete(sink.id)
      connectorClient.close()
    }
  }

  @Test
  def testUpdateRunningSink(): Unit = {
    val topicName = methodName
    val topic = Await.result(TopicApi
                               .access()
                               .hostname(configurator.hostname)
                               .port(configurator.port)
                               .add(TopicCreationRequest(topicName, 1, 1)),
                             10 seconds)
    val request = ConnectorConfigurationRequest(name = methodName,
                                                className = classOf[DumbSink].getName,
                                                schema = Seq.empty,
                                                topics = Seq(topic.id),
                                                numberOfTasks = 1,
                                                configs = Map.empty)

    val sink = result(access.add(request))
    // test start
    client.start[ConnectorConfiguration](sink.id)
    val connectorClient = ConnectorClient(testUtil.workersConnProps)
    try {
      CommonUtil.await(() =>
                         try connectorClient.exist(sink.id)
                         catch {
                           case _: Throwable => false
                       },
                       Duration.ofSeconds(30))
      CommonUtil
        .await(() => connectorClient.status(sink.id).connector.state == ConnectorState.RUNNING, Duration.ofSeconds(20))

      an[IllegalArgumentException] should be thrownBy result(access.update(sink.id, request.copy(numberOfTasks = 2)))
      an[IllegalArgumentException] should be thrownBy result(access.delete(sink.id))

      // test stop. the connector should be removed
      client.stop[ConnectorConfiguration](sink.id)
      CommonUtil.await(() => connectorClient.nonExist(sink.id), Duration.ofSeconds(20))
      result(access.get(sink.id)).state shouldBe None
    } finally {
      if (connectorClient.exist(sink.id)) connectorClient.delete(sink.id)
      connectorClient.close()
    }
  }

  @Test
  def testCommandMsg(): Unit = {
    val topicName = methodName
    val request = ConnectorConfigurationRequest(name = methodName,
                                                className = classOf[DumbSink].getName,
                                                schema = Seq.empty,
                                                topics = Seq(topicName),
                                                numberOfTasks = 1,
                                                configs = Map.empty)

    val fakeUUID = random()
    (the[IllegalArgumentException] thrownBy client.pause[ConnectorConfiguration](fakeUUID)).getMessage should include(
      "exist")
    (the[IllegalArgumentException] thrownBy client.resume[ConnectorConfiguration](fakeUUID)).getMessage should include(
      "exist")

    val sink = result(access.add(request))

    (the[IllegalArgumentException] thrownBy client.pause[ConnectorConfiguration](sink.id)).getMessage should include(
      "start")
    (the[IllegalArgumentException] thrownBy client.resume[ConnectorConfiguration](sink.id)).getMessage should include(
      "start")
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
