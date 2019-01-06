package com.island.ohara.configurator

import com.island.ohara.client.ConfiguratorClient
import com.island.ohara.client.configurator.v0.ConnectorApi.{ConnectorConfiguration, ConnectorConfigurationRequest}
import com.island.ohara.client.configurator.v0.PipelineApi.PipelineCreationRequest
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.client.configurator.v0.{ConnectorApi, PipelineApi, TopicApi}
import com.island.ohara.common.data.ConnectorState
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.ReleaseOnce
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
class TestPipelineRule extends SmallTest with Matchers {

  private[this] val configurator = Configurator.local()

  private[this] val client = ConfiguratorClient(configurator.hostname, configurator.port)

  private[this] val access = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)

  @Test
  def testPipelineStateAfterStartingSource(): Unit = {
    val topic = Await.result(TopicApi
                               .access()
                               .hostname(configurator.hostname)
                               .port(configurator.port)
                               .add(TopicCreationRequest(methodName, 1, 1)),
                             10 seconds)
    val sourceRequest = ConnectorConfigurationRequest(
      name = "abc",
      className = "jdbc",
      schema = Seq.empty,
      topics = Seq(topic.id),
      configs = Map.empty,
      numberOfTasks = 1
    )

    val source = Await.result(access.add(sourceRequest), 10 seconds)
    val pipeline = Await.result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          PipelineCreationRequest(
            name = "abc",
            rules = Map(source.id -> PipelineApi.UNKNOWN)
          )),
      10 seconds
    )
    pipeline.objects.foreach(obj => obj.state shouldBe None)

    // start source and pipeline should "see" what happen in source
    client.start[ConnectorConfiguration](source.id)
    val pipeline2 = Await
      .result(PipelineApi.access().hostname(configurator.hostname).port(configurator.port).get(pipeline.id), 10 seconds)
    pipeline2.objects.foreach(obj => obj.state.get shouldBe ConnectorState.RUNNING)
  }

  @Test
  def testUnknownObject(): Unit = {
    val sourceRequest = ConnectorConfigurationRequest(
      name = "abc",
      className = "jdbc",
      schema = Seq.empty,
      topics = Seq.empty,
      configs = Map.empty,
      numberOfTasks = 1
    )

    val source = Await.result(access.add(sourceRequest), 10 seconds)

    Await.result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          PipelineCreationRequest(
            name = "abc",
            rules = Map(source.id -> PipelineApi.UNKNOWN)
          )),
      10 seconds
    )

    val sinkRequest = ConnectorConfigurationRequest(
      name = "abc",
      className = "jdbc",
      schema = Seq.empty,
      topics = Seq.empty,
      configs = Map.empty,
      numberOfTasks = 1
    )

    val sink = Await.result(access.add(sinkRequest), 10 seconds)

    Await.result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          PipelineCreationRequest(
            name = "abc",
            rules = Map(PipelineApi.UNKNOWN -> sink.id)
          )),
      10 seconds
    )
  }

  @After
  def tearDown(): Unit = {
    ReleaseOnce.close(client)
    ReleaseOnce.close(configurator)
  }
}
