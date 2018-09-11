package com.island.ohara.configurator.endpoint

import com.island.ohara.integration.With3Brokers3Workers
import com.island.ohara.io.CloseOnce
import com.island.ohara.client.ConfiguratorJson.{FtpValidationRequest, HdfsValidationRequest, ValidationReport}
import com.island.ohara.kafka.KafkaClient
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

class TestValidator extends With3Brokers3Workers with Matchers {
  private[this] val taskCount = 3
  private[this] val kafkaClient = KafkaClient(testUtil.brokers)
  private[this] val ftpServer = testUtil.ftpServer

  @Before
  def setup(): Unit = {
    testUtil.connectorClient.plugins().exists(_.className == classOf[Validator].getName) shouldBe true
  }

  private[this] def evaluate(reports: Seq[ValidationReport]): Unit = {
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
        FtpValidationRequest(ftpServer.host,
                             ftpServer.port,
                             ftpServer.writableUser.name,
                             ftpServer.writableUser.password),
        taskCount
      ))
  }

  // TODO: add test against RDB. by chia

  @After
  def tearDown(): Unit = {
    CloseOnce.close(kafkaClient)
  }
}
