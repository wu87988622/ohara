package com.island.ohara.io

import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.mutable

class TestByteUtil extends SmallTest with Matchers {

  @Test
  def testConversionOfPrimitiveType(): Unit = {
    ByteUtil.toShort(ByteUtil.toBytes(1.toShort)) shouldBe 1
    ByteUtil.toInt(ByteUtil.toBytes(1)) shouldBe 1
    ByteUtil.toLong(ByteUtil.toBytes(1.toLong)) shouldBe 1
    ByteUtil.toFloat(ByteUtil.toBytes(1.toFloat)) shouldBe 1.0
    ByteUtil.toDouble(ByteUtil.toBytes(1.toDouble)) shouldBe 1.0
    ByteUtil.toString(ByteUtil.toBytes("ohara")) shouldBe "ohara"
    ByteUtil.toBoolean(ByteUtil.toBytes(false)) shouldBe false
    ByteUtil.toBoolean(ByteUtil.toBytes(true)) shouldBe true
  }

  @Test
  def testConversionOfPrimitiveTypeWithOffset(): Unit = {
    val buf = new Array[Byte](20)
    val init = 2
    var index = init
    ByteUtil.toBytes(1.toShort,
                     (b: Byte) =>
                       try buf.update(index, b)
                       finally index += 1)
    ByteUtil.toShort(buf, init) shouldBe 1

    index = init
    ByteUtil.toBytes(1,
                     (b: Byte) =>
                       try buf.update(index, b)
                       finally index += 1)
    ByteUtil.toInt(buf, init) shouldBe 1

    index = init
    ByteUtil.toBytes(1.toLong,
                     (b: Byte) =>
                       try buf.update(index, b)
                       finally index += 1)
    ByteUtil.toLong(buf, init) shouldBe 1

    index = init
    ByteUtil.toBytes(1.toFloat,
                     (b: Byte) =>
                       try buf.update(index, b)
                       finally index += 1)
    ByteUtil.toFloat(buf, init) shouldBe 1.0

    index = init
    ByteUtil.toBytes(1.toDouble,
                     (b: Byte) =>
                       try buf.update(index, b)
                       finally index += 1)
    ByteUtil.toDouble(buf, init) shouldBe 1.0

    index = init
    ByteUtil.toBytes("ohara",
                     (b: Byte) =>
                       try buf.update(index, b)
                       finally index += 1)
    ByteUtil.toString(buf, init, index - init) shouldBe "ohara"

    index = init
    ByteUtil.toBytes(true,
                     (b: Byte) =>
                       try buf.update(index, b)
                       finally index += 1)
    ByteUtil.toBoolean(buf, init) shouldBe true

    index = init
    ByteUtil.toBytes(false,
                     (b: Byte) =>
                       try buf.update(index, b)
                       finally index += 1)
    ByteUtil.toBoolean(buf, init) shouldBe false
  }

  @Test
  def testCompare(): Unit = {
    val a = "abc"
    val b = "aad"
    ByteUtil.compare(ByteUtil.toBytes(a), ByteUtil.toBytes(b)) shouldBe 1
    ByteUtil.compare(ByteUtil.toBytes(a), 2, 1, ByteUtil.toBytes(b), 2, 1) shouldBe -1
  }

  @Test
  def testComparator(): Unit = {
    val map = new mutable.TreeSet[Array[Byte]]()(ByteUtil.COMPARATOR)
    map += ByteUtil.toBytes("ccc")
    map += ByteUtil.toBytes("bbb")
    map += ByteUtil.toBytes("aaa")
    map += ByteUtil.toBytes("adc")
    map.size shouldBe 4
    map.zipWithIndex.foreach {
      case (value, index) =>
        index match {
          case 0 => ByteUtil.toString(value) shouldBe "aaa"
          case 1 => ByteUtil.toString(value) shouldBe "adc"
          case 2 => ByteUtil.toString(value) shouldBe "bbb"
          case 3 => ByteUtil.toString(value) shouldBe "ccc"
        }
    }
  }
}
