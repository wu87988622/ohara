package com.island.ohara.config

import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestOharaConfig extends SmallTest with Matchers {

  @Test
  def testRequire(): Unit = {
    val config = OharaConfig()
    the[IllegalArgumentException] thrownBy config.requireString("aa")
    the[IllegalArgumentException] thrownBy config.requireBoolean("aa")
    the[IllegalArgumentException] thrownBy config.requireDouble("aa")
    the[IllegalArgumentException] thrownBy config.requireFloat("aa")
    the[IllegalArgumentException] thrownBy config.requireInt("aa")
    the[IllegalArgumentException] thrownBy config.requireLong("aa")
    the[IllegalArgumentException] thrownBy config.requireShort("aa")

    config.set("aa", "value")
    config.requireString("aa") shouldBe "value"
    the[IllegalArgumentException] thrownBy config.requireBoolean("aa")
    the[IllegalArgumentException] thrownBy config.requireDouble("aa")
    the[IllegalArgumentException] thrownBy config.requireFloat("aa")
    the[IllegalArgumentException] thrownBy config.requireInt("aa")
    the[IllegalArgumentException] thrownBy config.requireLong("aa")
    the[IllegalArgumentException] thrownBy config.requireShort("aa")
  }

  @Test
  def testEquals(): Unit = {
    def selfCheck(config: OharaConfig): Unit = {
      config.equals(config) shouldBe true
      config.snapshot.equals(config) shouldBe true
      config.equals(config.snapshot) shouldBe true
      config.snapshot.equals(config.snapshot) shouldBe true
    }
    def checkEqual(lhs: OharaConfig, rhs: OharaConfig): Unit = {
      lhs.equals(rhs) shouldBe true
      rhs.equals(lhs) shouldBe true
    }

    def check(lhs: OharaConfig, rhs: OharaConfig): Unit = {
      selfCheck(lhs)
      selfCheck(rhs)
      checkEqual(lhs, rhs)
      checkEqual(lhs.snapshot, rhs)
      checkEqual(lhs, rhs.snapshot)
      checkEqual(lhs.snapshot, rhs.snapshot)
    }
    val conf = OharaConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None
    conf.set("key2", Map("aaa" -> "bb")) shouldBe None

    val conf2 = OharaConfig()
    conf2.set("key0", "value0") shouldBe None
    conf2.set("key1", "value1") shouldBe None
    conf2.set("key2", Map("aaa" -> "bb")) shouldBe None

    check(conf, conf2)

    check(OharaConfig(conf.toProperties), conf2)
    check(conf, OharaConfig(conf2.toProperties))
    check(OharaConfig(conf.toProperties), OharaConfig(conf2.toProperties))

    check(OharaConfig(conf.toJson), conf2)
    check(conf, OharaConfig(conf2.toJson))
    check(OharaConfig(conf.toJson), OharaConfig(conf2.toJson))

    val conf3 = OharaConfig()
    conf3.set("key0", "value0") shouldBe None
    conf3.set("key1", "value1") shouldBe None
    conf3.set("key2", "value2") shouldBe None

    conf3.equals(conf2) shouldBe false
    conf3.equals(conf) shouldBe false
    conf.equals(conf3) shouldBe false
    conf2.equals(conf3) shouldBe false
  }

  @Test
  def testGetStringAndMap(): Unit = {
    val conf = OharaConfig()
    conf.set("key0", "value0") shouldBe None
    conf.set("key1", "value1") shouldBe None
    conf.set("key2", Map("aaa" -> "bb")) shouldBe None

    conf.getString("key0") shouldBe Some("value0")
    conf.getString("key1") shouldBe Some("value1")
    the[IllegalArgumentException] thrownBy conf.getMap("key0")
    the[IllegalArgumentException] thrownBy conf.getMap("key1")

    conf.getMap("key2") shouldBe Some(Map("aaa" -> "bb"))
    the[IllegalArgumentException] thrownBy conf.getString("key2")

    conf.getString("abc") shouldBe None
    conf.getMap("abc") shouldBe None
  }
}
