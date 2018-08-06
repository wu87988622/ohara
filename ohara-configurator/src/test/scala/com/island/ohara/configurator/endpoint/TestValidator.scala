package com.island.ohara.configurator.endpoint

import com.island.ohara.configurator.endpoint.Validator._
import com.island.ohara.integration.With3Blockers3Workers
import com.island.ohara.io.CloseOnce
import com.island.ohara.rest.ConnectorClient
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

class TestValidator extends With3Blockers3Workers with Matchers {
  private[this] val taskCount = 3
  private[this] val connectorClient = ConnectorClient(testUtil.workersString)

  @Before
  def setup(): Unit = {
    connectorClient.existPlugin(classOf[Validator].getSimpleName) shouldBe true
  }

  private[this] def evaluate(reports: Seq[Report]): Unit = {
    reports.isEmpty shouldBe false
    reports.foreach(_.pass shouldBe true)
  }

  @Test
  def testValidateHdfs(): Unit = {
    evaluate(
      Validator
        .run(connectorClient, testUtil.brokersString, Map(TARGET -> TARGET_HDFS, URL -> "file:///tmp"), taskCount))
  }

  @Test
  def testValidateBroker(): Unit = {
    evaluate(
      Validator.run(connectorClient,
                    testUtil.brokersString,
                    Map(TARGET -> TARGET_BROKER, URL -> testUtil.brokersString),
                    taskCount))
  }

  // TODO: add test against RDB. by chia

  @After
  def tearDown(): Unit = {
    CloseOnce.close(connectorClient)
  }
}
