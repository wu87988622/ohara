package com.island.ohara.configurator
import com.island.ohara.client.ConfiguratorClient
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.configurator.store.Store
import com.island.ohara.io.CloseOnce
import com.island.ohara.rule.SmallTest
import org.junit.{After, Test}
import org.scalatest.Matchers

class TestOhara450 extends SmallTest with Matchers {

  private[this] val configurator =
    Configurator.builder().hostname("localhost").port(0).store(Store.inMemory[String, Any]).noCluster.build()

  private[this] val client = ConfiguratorClient(configurator.hostname, configurator.port)

  @Test
  def testUncreatablePipelin(): Unit = {
    val source = client.add[SourceRequest, Source](
      SourceRequest(name = "abc",
                    className = "aaa.class",
                    topics = Seq.empty,
                    numberOfTasks = 1,
                    schema = Seq.empty,
                    configs = Map.empty))
    val topic = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest("abc", 1, 1))
    val sink = client.add[SinkRequest, Sink](
      SinkRequest(name = "abc",
                  className = "aaa.class",
                  schema = Seq.empty,
                  configs = Map.empty,
                  topics = Seq.empty,
                  numberOfTasks = 1))

    client.list[Source].size shouldBe 1
    client.list[TopicInfo].size shouldBe 1
    client.list[Sink].size shouldBe 1

    client.add[PipelineRequest, Pipeline](
      PipelineRequest("abc", Map(source.uuid -> topic.uuid, topic.uuid -> sink.uuid)))
    client.list[Pipeline].size shouldBe 1
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(client)
    CloseOnce.close(configurator)
  }
}
