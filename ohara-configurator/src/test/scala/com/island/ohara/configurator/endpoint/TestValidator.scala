package com.island.ohara.configurator.endpoint

import com.island.ohara.integration.With3Brokers3Workers
import com.island.ohara.io.CloseOnce
import com.island.ohara.client.ConfiguratorJson.{HdfsValidationRequest, ValidationReport}
import com.island.ohara.kafka.KafkaClient
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

class TestValidator extends With3Brokers3Workers with Matchers {
  private[this] val taskCount = 3
  private[this] val connectorClient = testUtil.connectorClient()
  private[this] val kafkaClient = KafkaClient(testUtil.brokersString)

  @Before
  def setup(): Unit = {
    connectorClient.plugins().filter(_.className.equals(classOf[Validator].getName)).isEmpty shouldBe false
  }

  private[this] def evaluate(reports: Seq[ValidationReport]): Unit = {
    reports.isEmpty shouldBe false
    reports.foreach(_.pass shouldBe true)
  }

  @Test
  def testValidationOfHdfs(): Unit = {
    evaluate(Validator.run(connectorClient, kafkaClient, HdfsValidationRequest("file:///tmp"), taskCount))
  }

  // TODO: add test against RDB. by chia

  @After
  def tearDown(): Unit = {
    CloseOnce.close(connectorClient)
    CloseOnce.close(kafkaClient)
  }
}
