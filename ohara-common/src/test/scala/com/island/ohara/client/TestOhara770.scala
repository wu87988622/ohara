package com.island.ohara.client
import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestOhara770 extends SmallTest with Matchers {

  @Test
  def configsNameShouldBeRemoved(): Unit = {
    class DumbConnectorCreator extends ConnectorCreator {
      override protected def send(
        request: ConnectorJson.CreateConnectorRequest): ConnectorJson.CreateConnectorResponse = {
        request.config.get("name") shouldBe None
        null
      }
    }

    val creator = new DumbConnectorCreator()
    // this should pass
    creator.name("abc").connectorClass("asdasd").topic("aaa").config("name", "aa").create()
  }

}
