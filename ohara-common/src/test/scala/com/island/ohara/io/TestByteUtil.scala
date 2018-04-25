package com.island.ohara.io

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class TestByteUtil extends FlatSpec with Matchers {

  "The conversion" should "obey the symmetry rule" in {
    ByteUtil.toShort(ByteUtil.toBytes(1.toShort)) shouldBe 1
    ByteUtil.toInt(ByteUtil.toBytes(1)) shouldBe 1
    ByteUtil.toLong(ByteUtil.toBytes(1.toLong)) shouldBe 1
    ByteUtil.toFloat(ByteUtil.toBytes(1.toFloat)) shouldBe 1.0
    ByteUtil.toDouble(ByteUtil.toBytes(1.toDouble)) shouldBe 1.0
    ByteUtil.toString(ByteUtil.toBytes("ohara")) shouldBe "ohara"
    ByteUtil.toBoolean(ByteUtil.toBytes(false)) shouldBe false
    ByteUtil.toBoolean(ByteUtil.toBytes(true)) shouldBe true
  }

  "The conversion by range" should "obey the symmetry rule" in {
    val buf = new Array[Byte](20)
    val init = 2
    var index = init
    ByteUtil.toBytes(1.toShort, (b: Byte) => try buf.update(index, b) finally index += 1)
    ByteUtil.toShort(buf, init) shouldBe 1

    index = init
    ByteUtil.toBytes(1, (b: Byte) => try buf.update(index, b) finally index += 1)
    ByteUtil.toInt(buf, init) shouldBe 1

    index = init
    ByteUtil.toBytes(1.toLong, (b: Byte) => try buf.update(index, b) finally index += 1)
    ByteUtil.toLong(buf, init) shouldBe 1

    index = init
    ByteUtil.toBytes(1.toFloat, (b: Byte) => try buf.update(index, b) finally index += 1)
    ByteUtil.toFloat(buf, init) shouldBe 1.0

    index = init
    ByteUtil.toBytes(1.toDouble, (b: Byte) => try buf.update(index, b) finally index += 1)
    ByteUtil.toDouble(buf, init) shouldBe 1.0

    index = init
    ByteUtil.toBytes("ohara", (b: Byte) => try buf.update(index, b) finally index += 1)
    ByteUtil.toString(buf, init, index - init) shouldBe "ohara"

    index = init
    ByteUtil.toBytes(true, (b: Byte) => try buf.update(index, b) finally index += 1)
    ByteUtil.toBoolean(buf, init) shouldBe true

    index = init
    ByteUtil.toBytes(false, (b: Byte) => try buf.update(index, b) finally index += 1)
    ByteUtil.toBoolean(buf, init) shouldBe false
  }
}
