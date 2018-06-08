package com.island.ohara.configurator.job

import com.island.ohara.config.UuidUtil
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.CloseOnce
import com.island.ohara.rule.SmallTest
import com.island.ohara.serialization.BYTES
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._

class TestHttpJob extends SmallTest with Matchers {

  private[this] val util = new OharaTestUtil(3, 3)
  private[this] val topicName = "TestHttpJob"
  private[this] val server = new HttpJobServer(util.brokersString, UuidUtil.uuid(), topicName)
  private[this] val client = new HttpJobClient(util.brokersString, topicName)
  private[this] val jobRequest = HttpJobRequest.apply(RUN, "path", Map("a" -> BYTES), Map("a" -> "b"))
  private[this] val jobResponse = HttpJobResponse.apply(RUNNING, Map("a" -> "b"))

  @Test
  def testNormalRequestAndResponse(): Unit = {
    val response = client.request(jobRequest)
    util.await(() => server.countOfUndealtTasks == 1, 5 seconds)
    server.take().complete(jobResponse)
    Await.result(response, 5 seconds) match {
      case Right(r) => r shouldBe jobResponse
      case Left(e)  => throw new RuntimeException(e.message)
    }
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(client)
    CloseOnce.close(server)
    CloseOnce.close(util)
  }
}
