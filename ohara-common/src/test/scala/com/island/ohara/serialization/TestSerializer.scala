package com.island.ohara.serialization

import java.sql.{Date, Time, Timestamp}

import com.island.ohara.data.{Cell, Row}
import com.island.ohara.io.ByteUtil
import com.island.ohara.reflection.ReflectionUtil
import com.island.ohara.rule.SmallTest
import com.island.ohara.util.SystemUtil
import org.junit.Test
import org.scalatest.Matchers

class TestSerializer extends SmallTest with Matchers {

  private[this] val values = Seq[Any]("1", false, 1.toShort, 1, 1.toLong, 1.0.toFloat, 1.0, ByteUtil.toBytes("abc"))

  @Test
  def testReflection(): Unit = {
    ReflectionUtil.instantiate(RowSerializer.getClass.getName, classOf[Serializer[Row]])
    ReflectionUtil.instantiate(BooleanSerializer.getClass.getName, classOf[Serializer[Boolean]])
    ReflectionUtil.instantiate(ShortSerializer.getClass.getName, classOf[Serializer[Short]])
    ReflectionUtil.instantiate(IntSerializer.getClass.getName, classOf[Serializer[Int]])
    ReflectionUtil.instantiate(LongSerializer.getClass.getName, classOf[Serializer[Long]])
    ReflectionUtil.instantiate(FloatSerializer.getClass.getName, classOf[Serializer[Float]])
    ReflectionUtil.instantiate(DoubleSerializer.getClass.getName, classOf[Serializer[Double]])
    ReflectionUtil.instantiate(BytesSerializer.getClass.getName, classOf[Serializer[Array[Byte]]])
    ReflectionUtil.instantiate(StringSerializer.getClass.getName, classOf[Serializer[Array[String]]])
    ReflectionUtil.instantiate(ObjectSerializer.getClass.getName, classOf[Serializer[Array[Object]]])
  }

  @Test
  def testRowSerializer(): Unit = {
    0 until 10 foreach { _ =>
      val row = Row
        .builder()
        .tags(Set("tag0", "tag1"))
        .cells(values.zipWithIndex.map {
          case (v, cellIndex) => Cell(cellIndex.toString, v)
        })
        .build()
      row shouldBe RowSerializer.from(RowSerializer.to(row))
    }
  }

  @Test
  def testSerializableObject(): Unit = {
    val row = Row(
      Cell("c0", new Timestamp(SystemUtil.current())),
      Cell("c1", new Date(SystemUtil.current())),
      Cell("c2", new Time(SystemUtil.current())),
      Cell("c3", JustTest("123", 4))
    )

    val copy = Serializer.ROW.from(Serializer.ROW.to(row))
    copy shouldBe row
  }

  @Test
  def testNestedRow(): Unit = {
    val row = Row(
      Cell("a", 13),
      Cell("b",
           Row(
             Cell("c0", new Timestamp(SystemUtil.current())),
             Cell("c1", new Date(SystemUtil.current())),
             Cell("c3", JustTest("123", 4))
           )),
      Cell("c", false),
      Cell("d", "Asdasd"),
      Cell("e", 123.345)
    )
    val copy = Serializer.ROW.from(Serializer.ROW.to(row))
    copy shouldBe row
  }

  @Test
  def testPrimitiveSerializer(): Unit = {
    values.foreach {
      case v: String      => StringSerializer.from(StringSerializer.to(v)) shouldBe v
      case v: Boolean     => BooleanSerializer.from(BooleanSerializer.to(v)) shouldBe v
      case v: Short       => ShortSerializer.from(ShortSerializer.to(v)) shouldBe v
      case v: Int         => IntSerializer.from(IntSerializer.to(v)) shouldBe v
      case v: Long        => LongSerializer.from(LongSerializer.to(v)) shouldBe v
      case v: Float       => FloatSerializer.from(FloatSerializer.to(v)) shouldBe v
      case v: Double      => DoubleSerializer.from(DoubleSerializer.to(v)) shouldBe v
      case v: Array[Byte] => BytesSerializer.from(BytesSerializer.to(v)) shouldBe v
      case raw            => throw new IllegalArgumentException(s"Unsupported type:${raw.getClass.getCanonicalName}")
    }
  }
}

case class JustTest(name: String, value: Int)
