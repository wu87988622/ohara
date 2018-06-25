package com.island.ohara.config

import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestOharaPropertyBuilder extends SmallTest with Matchers {

  @Test
  def testBuildWithoutDefault(): Unit = {
    val builder = OharaProperty.builder.key("key").description("vvv")

    builder.booleanProperty.default shouldBe None
    builder.doubleProperty.default shouldBe None
    builder.floatProperty.default shouldBe None
    builder.intProperty.default shouldBe None
    builder.longProperty.default shouldBe None
    builder.shortProperty.default shouldBe None
    builder.stringProperty.default shouldBe None
    builder.mapProperty.default shouldBe None
  }

  @Test
  def testBuildProperties(): Unit = {
    val builder = OharaProperty.builder.key("key").description("desc")
    val list = List[Any](123, 123L, "123", 123D, 123F, true)
    list
      .map(_ match {
        case default: Short   => builder.shortProperty(default)
        case default: Int     => builder.intProperty(default)
        case default: Long    => builder.longProperty(default)
        case default: Float   => builder.floatProperty(default)
        case default: Double  => builder.doubleProperty(default)
        case default: Boolean => builder.booleanProperty(default)
        case default: String  => builder.stringProperty(default)
        case _                => throw new IllegalArgumentException
      })
      .foreach(prop => {
        prop.key shouldBe "key"
        prop.description shouldBe "desc"
        prop.default.map(_ match {
          case v: Boolean => v shouldBe true
          case v: Any     => v.toString.toDouble shouldBe 123.0
        })
      })
  }

  @Test
  def testMapBuilder(): Unit = {
    val property: OharaProperty[Map[String, Int]] =
      OharaProperty.builder.key("key").description("nothing").mapProperty(_.toInt, _.toString)
    an[UnsupportedOperationException] should be thrownBy property.from("xx")
    val value = property.from(Map("k0" -> "123", "k1" -> "456"))
    value.get("k0").get shouldBe 123
    value.get("k1").get shouldBe 456
  }

  @Test
  def testClear(): Unit = {
    val builder = OharaProperty.builder.key("key0").description("desc0")

    val property = builder.clear().key("key1").description("desc1").intProperty(100)

    property.key shouldBe "key1"
    property.description shouldBe "desc1"
    property.default.get shouldBe 100
  }

  @Test
  def testInvalidValue(): Unit = {
    an[IllegalArgumentException] should be thrownBy OharaProperty.builder
      .key("key")
      .description("dd")
      .stringProperty
      .require(null.asInstanceOf[String])

    an[IllegalArgumentException] should be thrownBy OharaProperty.builder
      .key("key")
      .description("dd")
      .property((a: String) => ("xx", "bb"), (a: (String, String)) => "xxx")
      .require(null.asInstanceOf[(String, String)])

    an[IllegalArgumentException] should be thrownBy OharaProperty.builder
      .key("key")
      .description("dd")
      .mapProperty
      .require(null.asInstanceOf[Map[String, String]])
  }
}
