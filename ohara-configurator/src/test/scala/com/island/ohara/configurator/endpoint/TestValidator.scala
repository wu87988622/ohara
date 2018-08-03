package com.island.ohara.configurator.endpoint

import com.island.ohara.configurator.endpoint.Validator._
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.CloseOnce
import com.island.ohara.rest.ConnectorClient
import com.island.ohara.rule.MediumTest
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

class TestValidator extends MediumTest with Matchers {
  private[this] val taskCount = 3
  private[this] val util = OharaTestUtil.localWorkers(taskCount, taskCount)
  private[this] val connectorClient = ConnectorClient(util.workersString)
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
      Validator.run(connectorClient, util.brokersString, Map(TARGET -> TARGET_HDFS, URL -> "file:///tmp"), taskCount))
  }

  @Test
  def testValidateBroker(): Unit = {
    evaluate(
      Validator
        .run(connectorClient, util.brokersString, Map(TARGET -> TARGET_BROKER, URL -> util.brokersString), taskCount))
  }

  // TODO: add test against RDB. by chia

  @After
  def tearDown(): Unit = {
    CloseOnce.close(connectorClient)
    util.close()
  }
}
