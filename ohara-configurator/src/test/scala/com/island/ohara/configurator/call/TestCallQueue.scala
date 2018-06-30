package com.island.ohara.configurator.call

import java.util.concurrent.{TimeUnit, TimeoutException}

import com.island.ohara.config.UuidUtil
import com.island.ohara.configurator.data.{OharaSource, OharaTarget}
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.CloseOnce.close
import com.island.ohara.kafka.KafkaUtil
import com.island.ohara.rule.LargeTest
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._

class TestCallQueue extends LargeTest with Matchers {

  private[this] val topicName = getClass.getSimpleName
  private[this] val util = OharaTestUtil.localBrokers(3)
  private[this] val groupId = UuidUtil.uuid()
  private[this] val defaultServerBuilder =
    CallQueue.serverBuilder.brokers(util.brokersString).topicName(topicName).groupId(groupId)
  private[this] val server0: CallQueueServer[OharaSource, OharaSource] =
    defaultServerBuilder.build[OharaSource, OharaSource]()
  private[this] val server1: CallQueueServer[OharaSource, OharaSource] =
    defaultServerBuilder.build[OharaSource, OharaSource]()
  private[this] val server2: CallQueueServer[OharaSource, OharaSource] =
    defaultServerBuilder.build[OharaSource, OharaSource]()
  private[this] val client: CallQueueClient[OharaSource, OharaSource] =
    CallQueue.clientBuilder.brokers(util.brokersString).topicName(topicName).build[OharaSource, OharaSource]()

  private[this] val servers = Seq(server0, server1, server2)

  private[this] val requestData: OharaSource = OharaSource.apply("uuid", "name", Map("a" -> "b"))
  private[this] val responseData: OharaSource = OharaSource.apply("uuid", "name2", Map("a" -> "b"))
  private[this] val error = new IllegalArgumentException("YOU SHOULD NOT PASS")

  @Test
  def testSingleRequestWithResponse(): Unit = {
    val request = client.request(requestData)
    // no task handler so it can't get any response
    an[TimeoutException] should be thrownBy Await.result(request, 3 second)

    // wait the one of servers receive the request
    OharaTestUtil.await(() => servers.map(_.countOfUndealtTasks).sum == 1, 10 second)

    // get the task and assign a response
    val task = servers.find(_.countOfUndealtTasks == 1).get.take()
    task.complete(responseData)
    Await.result(request, 3 second) shouldBe Right(responseData)
  }

  @Test
  def testSingleRequestWithFailure(): Unit = {
    val request = client.request(requestData)
    // no task handler so it can't get any response
    an[TimeoutException] should be thrownBy Await.result(request, 3 second)

    // wait the one of servers receive the request
    OharaTestUtil.await(() => servers.map(_.countOfUndealtTasks).sum == 1, 10 second)

    // get the task and assign a error
    val task = servers.find(_.countOfUndealtTasks == 1).get.take()
    task.complete(error)
    val result = Await.result(request, 3 second)
    result match {
      case Left(e) => e.message shouldBe error.getMessage
      case _       => throw new RuntimeException(s"receive a invalid result: $result")
    }
  }

  @Test
  def testSingleRequestWithTimeout(): Unit = {
    val request = client.request(requestData)
    // no task handler so it can't get any response
    an[TimeoutException] should be thrownBy Await.result(request, 3 second)

    // wait the one of servers receive the request
    OharaTestUtil.await(() => servers.map(_.countOfUndealtTasks).sum == 1, 10 second)

    // get the server accepting the request
    val server = servers.find(_.countOfUndealtTasks == 1).get
    server.close()
    // the server is closed so all undealt tasks should be assigned a identical error
    Await.result(request, 3 second) match {
      case Left(e) => e.message shouldBe CallQueue.TERMINATE_TIMEOUT_EXCEPTION.getMessage
      case _       => throw new RuntimeException("receive a invalid result")
    }
  }

  @Test
  def testSendInvalidRequest(): Unit = {
    val invalidClient: CallQueueClient[OharaTarget, OharaSource] = CallQueue.clientBuilder
      .brokers(util.brokersString)
      .topicName(topicName)
      .expirationCleanupTime(3 seconds)
      .build[OharaTarget, OharaSource]()
    try {
      val request = invalidClient.request(OharaTarget.apply("uuid", "name", Map("a" -> "b")))
      Await.result(request, 5 second) match {
        case Left(e) =>
          withClue(s"exception:${e.message}") {
            e.message.contains(classOf[OharaTarget].getName) shouldBe true
          }
        case _ => throw new RuntimeException("this request sent by this test should receive a exception")
      }

    } finally invalidClient.close()
  }

  @Test
  def testSendNoTopic(): Unit = {
    an[IllegalArgumentException] should be thrownBy CallQueue.clientBuilder
      .brokers(util.brokersString)
      .topicName("aNonExistedTopic")
      .build[OharaSource, OharaSource]()
  }

  @Test
  def testLease(): Unit = {
    val anotherTopic = "testLease"
    val leaseCleanupFreq: Duration = 5 seconds

    KafkaUtil.createTopicIfNotExist(util.brokersString, anotherTopic, 1, 1)
    val timeoutClient: CallQueueClient[OharaSource, OharaSource] = CallQueue.clientBuilder
      .brokers(util.brokersString)
      .topicName(anotherTopic)
      .expirationCleanupTime(leaseCleanupFreq)
      .build[OharaSource, OharaSource]()
    val request = timeoutClient.request(requestData, leaseCleanupFreq)
    TimeUnit.MILLISECONDS.sleep(leaseCleanupFreq.toMillis)
    Await.result(request, 5 second) match {
      case Left(e) => e.message shouldBe CallQueue.EXPIRED_REQUEST_EXCEPTION.getMessage
      case _       => throw new RuntimeException("this request sent by this test should receive a exception")
    }
  }

  @Test
  def testMultiRequest(): Unit = {
    val requestCount = 10
    val requests = 0 until requestCount map { _ =>
      client.request(requestData)
    }
    // wait the one of servers receive the request
    OharaTestUtil.await(() => servers.map(_.countOfUndealtTasks).sum == requestCount, 10 second)
    val tasks = servers.flatMap(server => {
      Iterator.continually(server.take(1 second)).takeWhile(_.isDefined).map(_.get)
    })
    tasks.size shouldBe requestCount

    tasks.foreach(_.complete(responseData))

    requests.foreach(Await.result(_, 10 seconds) match {
      case Right(r) => r shouldBe responseData
      case _        => throw new RuntimeException("All requests should work")
    })
  }

  @Test
  def testMultiRequestFromDifferentClients(): Unit = {
    val clientCount = 10
    val clients = 0 until clientCount map { _ =>
      CallQueue.clientBuilder.brokers(util.brokersString).topicName(topicName).build[OharaSource, OharaSource]()
    }
    val requests = clients.map(_.request(requestData))
    // wait the one of servers receive the request
    OharaTestUtil.await(() => servers.map(_.countOfUndealtTasks).sum == clientCount, 10 second)
    val tasks = servers.flatMap(server => {
      Iterator.continually(server.take(1 second)).takeWhile(_.isDefined).map(_.get)
    })
    tasks.size shouldBe clientCount

    tasks.foreach(_.complete(responseData))

    requests.foreach(Await.result(_, 10 seconds) match {
      case Right(r) => r shouldBe responseData
      case _        => throw new RuntimeException("All requests should work")
    })
  }

  @Test
  def testCloseClientWithOnFlyRequests(): Unit = {
    val requestCount = 10
    val topicName = testName.getMethodName
    KafkaUtil.createTopicIfNotExist(util.brokersString, topicName, 1, 1)
    val invalidClient: CallQueueClient[OharaSource, OharaSource] =
      CallQueue.clientBuilder.brokers(util.brokersString).topicName(topicName).build[OharaSource, OharaSource]()
    val requests = try 0 until requestCount map { _ =>
      invalidClient.request(requestData)
    } finally invalidClient.close()
    requests.foreach(Await.result(_, 15 seconds) match {
      case Left(exception) => exception.message shouldBe CallQueue.TERMINATE_TIMEOUT_EXCEPTION.getMessage
      case _               => throw new RuntimeException("All requests should fail")
    })
  }

  @After
  def tearDown(): Unit = {
    servers.foreach(close(_))
    close(client)
    close(util)
  }

}
