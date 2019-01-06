package com.island.ohara.configurator
import com.island.ohara.client.configurator.v0.Data
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorConfiguration
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.{CommonUtil, ReleaseOnce}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
class TestConfiguratorStore extends MediumTest with Matchers {

  private[this] val timeout = 10 seconds
  private[this] val store =
    new Configurator.Store(
      com.island.ohara.configurator.store.Store.inMemory(Serializer.STRING, Configurator.DATA_SERIALIZER))

  @Test
  def testAdd(): Unit = {
    val s = ConnectorConfiguration(
      id = "asdad",
      name = "abc",
      className = "aaa.class",
      schema = Seq.empty,
      topics = Seq("abc"),
      numberOfTasks = 1,
      configs = Map.empty,
      state = None,
      lastModified = CommonUtil.current()
    )
    Await.result(store.add(s), timeout)

    Await.result(store.exist[Data](s.id), timeout) shouldBe true
    Await.result(store.nonExist[Data](s.id), timeout) shouldBe false
  }

  @Test
  def testUpdate(): Unit = {
    val s = ConnectorConfiguration(
      id = "asdad",
      name = "abc",
      className = "aaa.class",
      schema = Seq.empty,
      topics = Seq("abc"),
      numberOfTasks = 1,
      configs = Map.empty,
      state = None,
      lastModified = CommonUtil.current()
    )
    store.add(s)

    Await.result(store.update(s.id, (_: Data) => s.copy(name = "123")), 10 seconds).name shouldBe "123"

    an[NoSuchElementException] should be thrownBy Await
      .result(store.update("asdasdasd", (_: Data) => s.copy(id = "123")), 10 seconds)
  }

  @Test
  def testList(): Unit = {
    val s = ConnectorConfiguration(
      id = "asdad",
      name = "abc",
      className = "aaa.class",
      schema = Seq.empty,
      topics = Seq("abc"),
      numberOfTasks = 1,
      configs = Map.empty,
      state = None,
      lastModified = CommonUtil.current()
    )
    store.add(s)

    store.size shouldBe 1

    Await.result(store.raw(), 10 seconds).head.asInstanceOf[ConnectorConfiguration] shouldBe s

    Await.result(store.raw(s.id), 10 seconds).asInstanceOf[ConnectorConfiguration] shouldBe s
  }

  @Test
  def testRemove(): Unit = {
    val s = ConnectorConfiguration(
      id = "asdad",
      name = "abc",
      className = "aaa.class",
      schema = Seq.empty,
      topics = Seq("abc"),
      numberOfTasks = 1,
      configs = Map.empty,
      state = None,
      lastModified = CommonUtil.current()
    )
    store.add(s)

    store.size shouldBe 1

    an[NoSuchElementException] should be thrownBy Await.result(store.remove("asdasd"), 50 seconds)
    an[NoSuchElementException] should be thrownBy Await.result(store.remove[ConnectorConfiguration]("asdasd"),
                                                               50 seconds)

    Await.result(store.remove[ConnectorConfiguration](s.id), 50 seconds) shouldBe s

    store.size shouldBe 0
  }

  @After
  def tearDown(): Unit = ReleaseOnce.close(store)

}
