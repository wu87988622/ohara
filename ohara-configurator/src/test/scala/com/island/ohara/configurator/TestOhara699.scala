package com.island.ohara.configurator

import com.island.ohara.client.ConfiguratorClient
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.ReleaseOnce
import com.island.ohara.configurator.store.Store
import org.junit.{After, Test}
import org.scalatest.Matchers

class TestOhara699 extends SmallTest with Matchers {

  private[this] val configurator =
    Configurator
      .builder()
      .hostname("localhost")
      .port(0)
      .store(Store.inMemory(Serializer.STRING, Serializer.OBJECT))
      .noCluster
      .build()

  private[this] val client = ConfiguratorClient(configurator.hostname, configurator.port)

  @Test
  def testStartAnNonexistantSource(): Unit = {
    an[IllegalArgumentException] should be thrownBy client.start[Source]("asdadasdas")
  }

  @Test
  def testStartAnNonexistantSink(): Unit = {
    an[IllegalArgumentException] should be thrownBy client.start[Sink]("asdadasdas")
  }

  @Test
  def testStopAnNonexistantSource(): Unit = {
    an[IllegalArgumentException] should be thrownBy client.stop[Source]("asdadasdas")
  }

  @Test
  def testStopAnNonexistantSink(): Unit = {
    an[IllegalArgumentException] should be thrownBy client.stop[Sink]("asdadasdas")
  }

  @Test
  def testPauseAnNonexistantSource(): Unit = {
    an[IllegalArgumentException] should be thrownBy client.pause[Source]("asdadasdas")
  }

  @Test
  def testPauseAnNonexistantSink(): Unit = {
    an[IllegalArgumentException] should be thrownBy client.pause[Sink]("asdadasdas")
  }

  @Test
  def testResumeAnNonexistantSource(): Unit = {
    an[IllegalArgumentException] should be thrownBy client.resume[Source]("asdadasdas")
  }

  @Test
  def testResumeAnNonexistantSink(): Unit = {
    an[IllegalArgumentException] should be thrownBy client.resume[Sink]("asdadasdas")
  }

  @After
  def tearDown(): Unit = {
    ReleaseOnce.close(client)
    ReleaseOnce.close(configurator)
  }
}
