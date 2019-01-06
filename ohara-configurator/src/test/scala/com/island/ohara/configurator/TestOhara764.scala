package com.island.ohara.configurator

import com.island.ohara.client.ConfiguratorClient
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.configurator.v0.ConnectorApi.{ConnectorConfiguration, ConnectorConfigurationRequest}
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.client.configurator.v0.{ConnectorApi, TopicApi}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.ReleaseOnce
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await

class TestOhara764 extends SmallTest with Matchers {

  private[this] val configurator = Configurator.local()

  private[this] val client = ConfiguratorClient(configurator.hostname, configurator.port)

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

    an[IllegalArgumentException] should be thrownBy client.start[ConnectorConfiguration](source.id)

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
    client.start[ConnectorConfiguration](source2.id)
  }

  @After
  def tearDown(): Unit = {
    ReleaseOnce.close(client)
    ReleaseOnce.close(configurator)
  }
}
