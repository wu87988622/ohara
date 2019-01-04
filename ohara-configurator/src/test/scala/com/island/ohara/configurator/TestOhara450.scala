package com.island.ohara.configurator
import com.island.ohara.client.ConfiguratorClient
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.ReleaseOnce
import org.junit.{After, Test}
import org.scalatest.Matchers

class TestOhara450 extends SmallTest with Matchers {

  private[this] val configurator = Configurator.local()

  private[this] val client = ConfiguratorClient(configurator.hostname, configurator.port)

  @Test
  def testUncreatablePipeline(): Unit = {
    val source = client.add[ConnectorConfigurationRequest, ConnectorConfiguration](
      ConnectorConfigurationRequest(name = "abc",
                                    className = "aaa.class",
                                    topics = Seq.empty,
                                    numberOfTasks = 1,
                                    schema = Seq.empty,
                                    configs = Map.empty))
    client.list[ConnectorConfiguration].size shouldBe 1
    val topic = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest("abc", 1, 1))
    val sink = client.add[ConnectorConfigurationRequest, ConnectorConfiguration](
      ConnectorConfigurationRequest(name = "abc",
                                    className = "aaa.class",
                                    schema = Seq.empty,
                                    configs = Map.empty,
                                    topics = Seq.empty,
                                    numberOfTasks = 1))

    client.list[ConnectorConfiguration].size shouldBe 2
    client.list[TopicInfo].size shouldBe 1

    client.add[PipelineRequest, Pipeline](PipelineRequest("abc", Map(source.id -> topic.id, topic.id -> sink.id)))
    client.list[Pipeline].size shouldBe 1
  }

  @After
  def tearDown(): Unit = {
    ReleaseOnce.close(client)
    ReleaseOnce.close(configurator)
  }
}
