package com.island.ohara.configurator
import com.island.ohara.client.ConfiguratorJson.{Data, Source}
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.{ReleaseOnce, CommonUtil}
import org.junit.{After, Test}
import org.scalatest.Matchers
class TestConfiguratorStore extends MediumTest with Matchers {

  private[this] val store =
    new Configurator.Store(com.island.ohara.configurator.store.Store.inMemory(Serializer.STRING, Serializer.OBJECT))

  @Test
  def testAdd(): Unit = {
    val s = Source(
      uuid = "asdad",
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

    store.exist[Data](s.uuid) shouldBe true
    store.nonExist[Data](s.uuid) shouldBe false
  }

  @Test
  def testUpdate(): Unit = {
    val s = Source(
      uuid = "asdad",
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

    val newOne = store.update(s.copy(name = "123"))
    newOne.name shouldBe "abc"

    an[IllegalArgumentException] should be thrownBy store.update(s.copy(uuid = "123"))
  }

  @Test
  def testList(): Unit = {
    val s = Source(
      uuid = "asdad",
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

    store.raw().toSeq.head.asInstanceOf[Source] shouldBe s

    store.raw(s.uuid).asInstanceOf[Source] shouldBe s
  }

  @Test
  def testRemove(): Unit = {
    val s = Source(
      uuid = "asdad",
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

    an[IllegalArgumentException] should be thrownBy store.remove("asdasd")
    an[IllegalArgumentException] should be thrownBy store.remove[Source]("asdasd")

    store.remove[Source](s.uuid) shouldBe s

    store.size shouldBe 0
  }

  @After
  def tearDown(): Unit = ReleaseOnce.close(store)

}
