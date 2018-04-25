package com.island.ohara.config

import java.util

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class TestTypesafeConfig extends FlatSpec with Matchers {
  "The basic getter/setter" should "work" in {
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
