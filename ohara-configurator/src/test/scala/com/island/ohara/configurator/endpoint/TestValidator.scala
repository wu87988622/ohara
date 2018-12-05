package com.island.ohara.configurator.endpoint

import com.island.ohara.client.ConfiguratorJson.{
  FtpValidationRequest,
  HdfsValidationRequest,
  RdbValidationRequest,
  ValidationReport
}
import com.island.ohara.client.ConnectorClient
import com.island.ohara.common.util.CloseOnce
import com.island.ohara.integration.With3Brokers3Workers
import com.island.ohara.kafka.KafkaClient
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestValidator extends With3Brokers3Workers with Matchers {
  private[this] val taskCount = 3
  private[this] val kafkaClient = KafkaClient.of(testUtil.brokersConnProps)
  private[this] val ftpServer = testUtil.ftpServer
  private[this] val rdb = testUtil.dataBase
  private[this] val connectorClient = ConnectorClient(testUtil.workersConnProps)

  @Before
  def setup(): Unit = {
    connectorClient.plugins().exists(_.className == classOf[Validator].getName) shouldBe true
  }

  private[this] def evaluate(f: Future[Seq[ValidationReport]]): Unit = {
    val reports = Await.result(f, 60 seconds)
    reports.isEmpty shouldBe false
    reports.foreach(_.pass shouldBe true)
  }

  @Test
  def testValidationOfHdfs(): Unit = {
    evaluate(Validator.run(connectorClient, kafkaClient, HdfsValidationRequest("file:///tmp"), taskCount))
  }

  @Test
  def testValidationOfFtp(): Unit = {
    evaluate(
      Validator.run(
        connectorClient,
        kafkaClient,
        FtpValidationRequest(ftpServer.hostname, ftpServer.port, ftpServer.user, ftpServer.password),
        taskCount
      ))
  }

  @Test
  def testValidationOfRdb(): Unit = {
    evaluate(
      Validator.run(
        connectorClient,
        kafkaClient,
        RdbValidationRequest(rdb.url, rdb.user, rdb.password),
        taskCount
      ))
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(connectorClient)
    CloseOnce.close(kafkaClient)
  }
}
