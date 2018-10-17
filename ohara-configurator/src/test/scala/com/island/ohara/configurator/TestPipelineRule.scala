package com.island.ohara.configurator

import com.island.ohara.client.{ConfiguratorClient, ConfiguratorJson}
import com.island.ohara.client.ConfiguratorJson.{Pipeline, PipelineRequest, Sink, SinkRequest, Source, SourceRequest}
import com.island.ohara.io.CloseOnce
import com.island.ohara.rule.SmallTest
import org.junit.{After, Test}
import org.scalatest.Matchers

class TestPipelineRule extends SmallTest with Matchers {

  private[this] val configurator = Configurator.builder().noCluster.hostname("localhost").port(0).build()

  private[this] val client = ConfiguratorClient(configurator.hostname, configurator.port)

  @Test
  def testUnknownObject(): Unit = {
    val sourceRequest = SourceRequest(
      name = "abc",
      className = "jdbc",
      schema = Seq.empty,
      topics = Seq.empty,
      configs = Map.empty,
      numberOfTasks = 1
    )

    val source = client.add[SourceRequest, Source](sourceRequest)

    client.add[PipelineRequest, Pipeline](
      PipelineRequest(
        name = "abc",
        rules = Map(source.uuid -> ConfiguratorJson.UNKNOWN)
      ))

    val sinkRequest = SinkRequest(
      name = "abc",
      className = "jdbc",
      schema = Seq.empty,
      topics = Seq.empty,
      configs = Map.empty,
      numberOfTasks = 1
    )

    val sink = client.add[SinkRequest, Sink](sinkRequest)

    client.add[PipelineRequest, Pipeline](
      PipelineRequest(
        name = "abc",
        rules = Map(ConfiguratorJson.UNKNOWN -> sink.uuid)
      ))
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(client)
    CloseOnce.close(configurator)
  }
}
