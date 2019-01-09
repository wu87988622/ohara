package com.island.ohara.configurator

import com.island.ohara.client.configurator.v0.ConnectorApi
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.ReleaseOnce
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
class TestOhara699 extends SmallTest with Matchers {

  private[this] val configurator = Configurator.fake()

  private[this] val access = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)

  @Test
  def testStartAnNonexistentConnector(): Unit = {
    an[IllegalArgumentException] should be thrownBy Await.result(access.start(methodName()), 30 seconds)
  }

  @Test
  def testStopAnNonexistentConnector(): Unit = {
    an[IllegalArgumentException] should be thrownBy Await.result(access.stop(methodName()), 30 seconds)
  }
  @Test
  def testPauseAnNonexistentConnector(): Unit = {
    an[IllegalArgumentException] should be thrownBy Await.result(access.pause(methodName()), 30 seconds)
  }

  @Test
  def testResumeAnNonexistentConnector(): Unit = {
    an[IllegalArgumentException] should be thrownBy Await.result(access.resume(methodName()), 30 seconds)
  }

  @After
  def tearDown(): Unit = ReleaseOnce.close(configurator)
}
