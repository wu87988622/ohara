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
    conf.set("key0", "value0") shouldBe Some("valueX")
    conf.get("key0") shouldBe Some("value0")
    conf.get("key1") shouldBe Some("value1")

    val copy = conf.snapshot
    copy.get("key0") shouldBe Some("value0")
    copy.get("key1") shouldBe Some("value1")
  }

  @Test
  def testJson(): Unit = {
    val conf = new MapConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None
    conf.get("key0") shouldBe Some("value0")
    conf.get("key1") shouldBe Some("value1")
    val json = conf.toJson
    val copy = MapConfig(json)
    copy.get("key0") shouldBe Some("value0")
    copy.get("key1") shouldBe Some("value1")
  }

  @Test
  def testProperties(): Unit = {
    val conf = new MapConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None
    conf.get("key0") shouldBe Some("value0")
    conf.get("key1") shouldBe Some("value1")
    val props = conf.toProperties
    props.getProperty("key0") shouldBe "value0"
    props.getProperty("key1") shouldBe "value1"

    val newOne = MapConfig(props)
    newOne.get("key0") shouldBe Some("value0")
    newOne.get("key1") shouldBe Some("value1")
  }

  @Test
  def testMergeProperties(): Unit = {
    val conf = new MapConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None

    val props = new Properties()
    props.setProperty("key2", "value2")

    val newOne = conf.merge(props)
    newOne.requireString("key0") shouldBe "value0"
    newOne.requireString("key1") shouldBe "value1"
    newOne.requireString("key2") shouldBe "value2"
  }

  @Test
  def testMergeConfig(): Unit = {
    val conf = new MapConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None

    val props = new MapConfig()
    props.set("key2", "value2")

    val newOne = conf.merge(props)
    newOne.requireString("key0") shouldBe "value0"
    newOne.requireString("key1") shouldBe "value1"
    newOne.requireString("key2") shouldBe "value2"
  }

  @Test
  def testMergeJson(): Unit = {
    val conf = new MapConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None

    val props = new MapConfig()
    props.set("key2", "value2")

    val newOne = conf.merge(props.toJson)
    newOne.requireString("key0") shouldBe "value0"
    newOne.requireString("key1") shouldBe "value1"
    newOne.requireString("key2") shouldBe "value2"
  }

  @Test
  def testLoadProperties(): Unit = {
    val conf = new MapConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None

    val props = new Properties()
    props.setProperty("key2", "value2")

    conf.load(props)
    conf.requireString("key0") shouldBe "value0"
    conf.requireString("key1") shouldBe "value1"
    conf.requireString("key2") shouldBe "value2"
  }

  @Test
  def testLoadConfig(): Unit = {
    val conf = new MapConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None

    val props = new MapConfig()
    props.set("key2", "value2")

    conf.load(props)
    conf.requireString("key0") shouldBe "value0"
    conf.requireString("key1") shouldBe "value1"
    conf.requireString("key2") shouldBe "value2"
  }

  @Test
  def testLoadJson(): Unit = {
    val conf = new MapConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None

    val props = new MapConfig()
    props.set("key2", "value2")

    conf.load(props.toJson)
    conf.requireString("key0") shouldBe "value0"
    conf.requireString("key1") shouldBe "value1"
    conf.requireString("key2") shouldBe "value2"
  }
}
