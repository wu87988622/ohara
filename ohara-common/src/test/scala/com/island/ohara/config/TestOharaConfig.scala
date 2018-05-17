package com.island.ohara.config

import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestOharaConfig extends SmallTest with Matchers {

  @Test
  def testRequrie(): Unit = {
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
}
