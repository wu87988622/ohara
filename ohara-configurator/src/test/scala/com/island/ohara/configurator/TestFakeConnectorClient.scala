package com.island.ohara.configurator
import com.island.ohara.common.data.ConnectorState
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestFakeConnectorClient extends SmallTest with Matchers {

  @Test
  def testControlConnector(): Unit = {
    val connectorName = methodName
    val topicName = methodName
    val className = methodName
    val fake = new FakeConnectorClient()
    fake.connectorCreator().name(connectorName).topic(topicName).numberOfTasks(1).connectorClass(className).create()

    fake.exist(connectorName) shouldBe true

    fake.status(connectorName).connector.state shouldBe ConnectorState.RUNNING

    fake.pause(connectorName)
    fake.status(connectorName).connector.state shouldBe ConnectorState.PAUSED

    fake.resume(connectorName)
    fake.status(connectorName).connector.state shouldBe ConnectorState.RUNNING

    fake.delete(connectorName)
    fake.exist(connectorName) shouldBe false
  }
}
