package com.island.ohara.configurator
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorConfigurationRequest
import com.island.ohara.client.configurator.v0.PipelineApi.PipelineCreationRequest
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.client.configurator.v0.{ConnectorApi, PipelineApi, TopicApi}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.ReleaseOnce
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await

class TestOhara450 extends SmallTest with Matchers {

  private[this] val configurator = Configurator.local()

  private[this] val access = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)

  import scala.concurrent.duration._
  @Test
  def testUncreatablePipeline(): Unit = {
    val source = Await.result(
      access.add(
        ConnectorConfigurationRequest(name = "abc",
                                      className = "aaa.class",
                                      topics = Seq.empty,
                                      numberOfTasks = 1,
                                      schema = Seq.empty,
                                      configs = Map.empty)),
      10 seconds
    )
    Await.result(access.list(), 10 seconds).size shouldBe 1
    import scala.concurrent.duration._
    val topic = Await.result(
      TopicApi.access().hostname(configurator.hostname).port(configurator.port).add(TopicCreationRequest("abc", 1, 1)),
      10 seconds)
    val sink = Await.result(
      access.add(
        ConnectorConfigurationRequest(name = "abc",
                                      className = "aaa.class",
                                      schema = Seq.empty,
                                      configs = Map.empty,
                                      topics = Seq.empty,
                                      numberOfTasks = 1)),
      10 seconds
    )
    Await.result(access.list(), 10 seconds).size shouldBe 2
    Await
      .result(TopicApi.access().hostname(configurator.hostname).port(configurator.port).list(), 10 seconds)
      .size shouldBe 1

    Await.result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          PipelineCreationRequest(
            "abc",
            Map(source.id -> topic.id, topic.id -> sink.id)
          )),
      10 seconds
    )
    Await
      .result(PipelineApi.access().hostname(configurator.hostname).port(configurator.port).list(), 10 seconds)
      .size shouldBe 1
  }

  @After
  def tearDown(): Unit = ReleaseOnce.close(configurator)
}
