package com.island.ohara.configurator

import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorConfigurationRequest
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.client.configurator.v0.{ConnectorApi, TopicApi}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.ReleaseOnce
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await

class TestOhara764 extends SmallTest with Matchers {

  private[this] val configurator = Configurator.fake()

  import scala.concurrent.duration._
  @Test
  def testStartSourceWithoutExistentTopic(): Unit = {
    val access = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)
    val source = Await.result(
      access.add(
        ConnectorConfigurationRequest(name = "abc",
                                      className = "aaa.class",
                                      topics = Seq("abc"),
                                      numberOfTasks = 1,
                                      schema = Seq.empty,
                                      configs = Map.empty)),
      10 seconds
    )

    an[IllegalArgumentException] should be thrownBy Await.result(access.start(source.id), 30 seconds)

    val topic = Await.result(
      TopicApi.access().hostname(configurator.hostname).port(configurator.port).add(TopicCreationRequest("abc", 1, 1)),
      10 seconds)
    val source2 = Await.result(
      access.add(
        ConnectorConfigurationRequest(name = "abc",
                                      className = "aaa.class",
                                      topics = Seq(topic.id),
                                      numberOfTasks = 1,
                                      schema = Seq.empty,
                                      configs = Map.empty)),
      10 seconds
    )
    Await.result(access.start(source2.id), 30 seconds).id shouldBe source2.id
  }

  @After
  def tearDown(): Unit = ReleaseOnce.close(configurator)
}
