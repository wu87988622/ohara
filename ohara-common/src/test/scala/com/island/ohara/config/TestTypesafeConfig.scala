package com.island.ohara.config

import java.util

import com.island.ohara.rule.SmallTest
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.junit.Test
import org.scalatest.Matchers

class TestTypesafeConfig extends SmallTest with Matchers {

  @Test
  def testGetterAndSetter():Unit = {
    val conf = ConfigFactory.empty()
      .withValue("key", ConfigValueFactory.fromAnyRef("value"))
      .withValue("key2", ConfigValueFactory.fromIterable(util.Arrays.asList("v0", "v1")))
    conf.entrySet().size() shouldBe 2
    conf.getString("key") shouldBe "value"
    conf.getStringList("key2").size() shouldBe 2
    conf.getStringList("key2").get(0) shouldBe "v0"
    conf.getStringList("key2").get(1) shouldBe "v1"
  }
}
