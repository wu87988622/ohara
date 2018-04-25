package com.island.ohara.config

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class TestPropertyBuilder extends FlatSpec with Matchers {

  "The build of Property" should "work" in {
    val builder = Property.builder
      .key("key")
      .description("desc")
    val list = List(123, 123L, "123", 123D, 123F, true)
    list.map(_ match {
      case default: Short => builder.build(default)
      case default: Int => builder.build(default)
      case default: Long => builder.build(default)
      case default: Float => builder.build(default)
      case default: Double => builder.build(default)
      case default: Boolean => builder.build(default)
      case default: String => builder.build(default)
      case _ => throw new IllegalArgumentException
    }).foreach((prop: Property[_]) => {
      prop.key shouldBe "key"
      prop.description shouldBe "desc"
      prop.default match {
        case v: Boolean => v shouldBe true
        case _ => prop.default.toString.toDouble shouldBe 123.0
      }
    })
  }
}
