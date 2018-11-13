package com.island.ohara.configurator.endpoint

import com.island.ohara.integration.With3Brokers3Workers
import com.island.ohara.client.util.CloseOnce
import com.island.ohara.client.ConfiguratorJson.{
  FtpValidationRequest,
  HdfsValidationRequest,
  RdbValidationRequest,
  ValidationReport
}
import com.island.ohara.kafka.KafkaClient
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
class TestValidator extends With3Brokers3Workers with Matchers {
  private[this] val taskCount = 3
  private[this] val kafkaClient = KafkaClient(testUtil.brokersConnProps)
  private[this] val ftpServer = testUtil.ftpServer
  private[this] val rdb = testUtil.dataBase

  @Before
  def setup(): Unit = {
    testUtil.connectorClient.plugins().exists(_.className == classOf[Validator].getName) shouldBe true
  }

  private[this] def evaluate(f: Future[Seq[ValidationReport]]): Unit = {
    val reports = Await.result(f, 60 seconds)
    reports.isEmpty shouldBe false
    reports.foreach(_.pass shouldBe true)
  }

  @Test
  def testValidationOfHdfs(): Unit = {
    evaluate(Validator.run(testUtil.connectorClient, kafkaClient, HdfsValidationRequest("file:///tmp"), taskCount))
  }

  @Test
  def testValidationOfFtp(): Unit = {
    evaluate(
      Validator.run(
        testUtil.connectorClient,
        kafkaClient,
        FtpValidationRequest(ftpServer.host, ftpServer.port, ftpServer.user, ftpServer.password),
        taskCount
      ))
  }

  @Test
  def testValidationOfRdb(): Unit = {
    evaluate(
      Validator.run(
        testUtil.connectorClient,
        kafkaClient,
        RdbValidationRequest(rdb.url, rdb.user, rdb.password),
        taskCount
      ))
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(kafkaClient)
  }
}
