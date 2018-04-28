package com.island.ohara.core

import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestRow extends SmallTest with Matchers {

  @Test
  def testBuilderRow():Unit = {
    val list = List(123.toShort, 123, 123L, "123", 123D, 123F, true)
    val row = Row(list.map(_ match {
      case v: Boolean => Cell.builder.name("boolean").build(v)
      case v: Short => Cell.builder.name("short").build(v)
      case v: Int => Cell.builder.name("int").build(v)
      case v: Long => Cell.builder.name("long").build(v)
      case v: Float => Cell.builder.name("float").build(v)
      case v: Double => Cell.builder.name("double").build(v)
      case v: String => Cell.builder.name("string").build(v)
      case _ => throw new IllegalArgumentException
    }))
    row.size shouldBe list.size
    row.names.size shouldBe list.size
    row seekCell "boolean" shouldBe defined
    row seekCell "short" shouldBe defined
    row seekCell "int" shouldBe defined
    row seekCell "long" shouldBe defined
    row seekCell "float" shouldBe defined
    row seekCell "double" shouldBe defined
    row seekCell "string" shouldBe defined
  }
}
