package com.island.ohara.configurator

import com.island.ohara.client.configurator.v0.NodeApi
import com.island.ohara.client.configurator.v0.NodeApi.NodeCreationRequest
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.ReleaseOnce
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
class TestOhara1255 extends SmallTest with Matchers {

  private[this] val configurator =
    Configurator.builder().kafkaClient(new FakeKafkaClient()).connectClient(new FakeConnectorClient()).build()

  private[this] val access = NodeApi.access().hostname("localhost").port(configurator.port)

  @Test
  def testAddInvalidNodes(): Unit = {
    val numberOfRequest = 10
    (0 until numberOfRequest)
      .map(
        i =>
          NodeCreationRequest(
            name = Some(i.toString),
            port = i,
            user = i.toString,
            password = i.toString
        ))
      .foreach(r => Await.result(access.add(r), 10 seconds))

    Await.result(access.list(), 10 seconds).size shouldBe numberOfRequest
  }

  @After
  def tearDown(): Unit = ReleaseOnce.close(configurator)
}
