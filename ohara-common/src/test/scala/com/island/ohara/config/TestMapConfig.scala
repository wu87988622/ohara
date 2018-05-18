package com.island.ohara.config

import java.util.Properties

import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestMapConfig extends SmallTest with Matchers {

  @Test
  def testGetterAndSetter(): Unit = {
    val conf = new MapConfig()
    conf.set("key0", "valueX") shouldBe None
    conf.set("key1", "value1") shouldBe None
    conf.set("key0", "value0") shouldBe Some(Left("valueX"))
    conf.get("key0") shouldBe Some(Left("value0"))
    conf.get("key1") shouldBe Some(Left("value1"))

    val copy = conf.snapshot
    copy.get("key0") shouldBe Some(Left("value0"))
    copy.get("key1") shouldBe Some(Left("value1"))
  }

  @Test
  def testJson(): Unit = {
    val conf = new MapConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None
    conf.set("key2", Map("aaa" -> "bb")) shouldBe None
    conf.get("key0") shouldBe Some(Left("value0"))
    conf.get("key1") shouldBe Some(Left("value1"))
    conf.get("key2") shouldBe Some(Right(Map("aaa" -> "bb")))
    val json = conf.toJson
    val copy = MapConfig(json)
    copy.get("key0") shouldBe Some(Left("value0"))
    copy.get("key1") shouldBe Some(Left("value1"))
    conf.get("key2") shouldBe Some(Right(Map("aaa" -> "bb")))
  }

  @Test
  def testProperties(): Unit = {
    val conf = new MapConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None
    conf.set("key2", Map("aaa" -> "bb")) shouldBe None
    conf.get("key0") shouldBe Some(Left("value0"))
    conf.get("key1") shouldBe Some(Left("value1"))
    conf.get("key2") shouldBe Some(Right(Map("aaa" -> "bb")))
    val props = conf.toProperties
    props.getProperty("key0") shouldBe "value0"
    props.getProperty("key1") shouldBe "value1"
    props.get("key2") shouldBe Map("aaa" -> "bb")

    val newOne = MapConfig(props)
    newOne.get("key0") shouldBe Some(Left("value0"))
    newOne.get("key1") shouldBe Some(Left("value1"))
    conf.get("key2") shouldBe Some(Right(Map("aaa" -> "bb")))
  }

  @Test
  def testMergeProperties(): Unit = {
    val conf = new MapConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None
    conf.set("key2", Map("aaa" -> "bb")) shouldBe None

    val props = new Properties()
    props.setProperty("key3", "value3")
    props.put("key4", Map("aaa" -> "bb"))

    val newOne = conf.merge(props)
    newOne.requireString("key0") shouldBe "value0"
    newOne.requireString("key1") shouldBe "value1"
    newOne.requireMap("key2") shouldBe Map("aaa" -> "bb")
    newOne.requireString("key3") shouldBe "value3"
    newOne.requireMap("key4") shouldBe Map("aaa" -> "bb")
  }

  @Test
  def testMergeConfig(): Unit = {
    val conf = new MapConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None
    conf.set("key2", Map("aaa" -> "bb")) shouldBe None

    val props = new MapConfig()
    props.set("key3", "value3")
    props.set("key4", Map("aaa" -> "bb"))

    val newOne = conf.merge(props)
    newOne.requireString("key0") shouldBe "value0"
    newOne.requireString("key1") shouldBe "value1"
    newOne.requireMap("key2") shouldBe Map("aaa" -> "bb")
    newOne.requireString("key3") shouldBe "value3"
    newOne.requireMap("key4") shouldBe Map("aaa" -> "bb")
  }

  @Test
  def testMergeJson(): Unit = {
    val conf = new MapConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None
    conf.set("key2", Map("aaa" -> "bb")) shouldBe None

    val props = new MapConfig()
    props.set("key3", "value3")
    props.set("key4", Map("aaa" -> "bb"))

    val newOne = conf.merge(props.toJson)
    newOne.requireString("key0") shouldBe "value0"
    newOne.requireString("key1") shouldBe "value1"
    newOne.requireMap("key2") shouldBe Map("aaa" -> "bb")
    newOne.requireString("key3") shouldBe "value3"
    newOne.requireMap("key4") shouldBe Map("aaa" -> "bb")
  }

  @Test
  def testLoadProperties(): Unit = {
    val conf = new MapConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None
    conf.set("key2", Map("aaa" -> "bb")) shouldBe None

    val props = new Properties()
    props.setProperty("key3", "value3")
    props.put("key4", Map("aaa" -> "bb"))

    conf.load(props)
    conf.requireString("key0") shouldBe "value0"
    conf.requireString("key1") shouldBe "value1"
    conf.requireMap("key2") shouldBe Map("aaa" -> "bb")
    conf.requireString("key3") shouldBe "value3"
    conf.requireMap("key4") shouldBe Map("aaa" -> "bb")
  }

  @Test
  def testLoadConfig(): Unit = {
    val conf = new MapConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None
    conf.set("key2", Map("aaa" -> "bb")) shouldBe None

    val props = new MapConfig()
    props.set("key3", "value3")
    props.set("key4", Map("aaa" -> "bb"))

    conf.load(props)
    conf.requireString("key0") shouldBe "value0"
    conf.requireString("key1") shouldBe "value1"
    conf.requireMap("key2") shouldBe Map("aaa" -> "bb")
    conf.requireString("key3") shouldBe "value3"
    conf.requireMap("key4") shouldBe Map("aaa" -> "bb")
  }

  @Test
  def testLoadJson(): Unit = {
    val conf = new MapConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None
    conf.set("key2", Map("aaa" -> "bb")) shouldBe None

    val props = new MapConfig()
    props.set("key3", "value3")
    props.set("key4", Map("aaa" -> "bb", "dd" -> "cc"))
    props.requireMap("key4") shouldBe Map("aaa" -> "bb", "dd" -> "cc")

    conf.load(props.toJson)
    conf.requireString("key0") shouldBe "value0"
    conf.requireString("key1") shouldBe "value1"
    conf.requireMap("key2") shouldBe Map("aaa" -> "bb")
    conf.requireString("key3") shouldBe "value3"
    conf.requireMap("key4") shouldBe Map("aaa" -> "bb", "dd" -> "cc")
  }

  @Test
  def testIterable(): Unit = {
    val conf = new MapConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None
    conf.set("key2", Map("aaa" -> "bb")) shouldBe None
    conf.set("key3", "value3") shouldBe None
    conf.set("key4", Map("aaa" -> "bb", "dd" -> "cc")) shouldBe None
    conf.foreach {
      case (key, value) =>
        key match {
          case "key0" => value shouldBe Left("value0")
          case "key1" => value shouldBe Left("value1")
          case "key2" => value shouldBe Right(Map("aaa" -> "bb"))
          case "key3" => value shouldBe Left("value3")
          case "key4" => value shouldBe Right(Map("aaa" -> "bb", "dd" -> "cc"))
          case _      => throw new UnknownError()
        }
    }
  }

}
