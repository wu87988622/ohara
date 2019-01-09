package com.island.ohara.configurator

import com.island.ohara.client.ConfiguratorClient
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorConfiguration
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.ReleaseOnce
import org.junit.{After, Test}
import org.scalatest.Matchers

class TestOhara699 extends SmallTest with Matchers {

  private[this] val configurator = Configurator.fake()

  private[this] val client = ConfiguratorClient(configurator.hostname, configurator.port)

  @Test
  def testStartAnNonexistentConnector(): Unit = {
    an[IllegalArgumentException] should be thrownBy client.start[ConnectorConfiguration]("asdadasdas")
  }

  @Test
  def testStopAnNonexistentConnector(): Unit = {
    an[IllegalArgumentException] should be thrownBy client.stop[ConnectorConfiguration]("asdadasdas")
  }
  @Test
  def testPauseAnNonexistentConnector(): Unit = {
    an[IllegalArgumentException] should be thrownBy client.pause[ConnectorConfiguration]("asdadasdas")
  }

  @Test
  def testResumeAnNonexistentConnector(): Unit = {
    an[IllegalArgumentException] should be thrownBy client.resume[ConnectorConfiguration]("asdadasdas")
  }

  @After
  def tearDown(): Unit = {
    ReleaseOnce.close(client)
    ReleaseOnce.close(configurator)
  }
}
