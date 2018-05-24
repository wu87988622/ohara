package com.island.ohara.io

import java.nio.charset.StandardCharsets
import java.util.Comparator

/**
  * A collection of helper methods used to do conversion between object and byte array.
  */
object ByteUtil {

  /**
    * used to sort the byte array in collection.
    */
  val COMPARATOR = new Comparator[Array[Byte]]() {
    override def compare(o1: Array[Byte], o2: Array[Byte]): Int = ByteUtil.compare(o1, o2)
  }

  // pre-defined constants
  def SIZE_OF_BYTE: Int = java.lang.Byte.SIZE / java.lang.Byte.SIZE

  def SIZE_OF_SHORT: Int = java.lang.Short.SIZE / java.lang.Byte.SIZE

  def SIZE_OF_INT: Int = java.lang.Integer.SIZE / java.lang.Byte.SIZE

  def SIZE_OF_LONG: Int = java.lang.Long.SIZE / java.lang.Byte.SIZE

  def SIZE_OF_FLOAT: Int = SIZE_OF_INT

  def SIZE_OF_DOUBLE: Int = SIZE_OF_LONG

  // -------------[string]------------- //
  implicit def toBytes(value: String): Array[Byte] = value.getBytes(StandardCharsets.UTF_8)

  implicit def toBytes(value: String, f: Byte => Unit): Unit = value.getBytes(StandardCharsets.UTF_8).foreach(f)

  implicit def toString(value: Array[Byte]): String = toString(value, 0, value.length)

  implicit def toString(value: Array[Byte], offset: Int, length: Int): String =
    new String(value, offset, length, StandardCharsets.UTF_8)

  // -------------[long]------------- //
  implicit def toBytes(value: Long): Array[Byte] = {
    val buf = new Array[Byte](SIZE_OF_LONG)
    var index = 0
    toBytes(value, (b: Byte) => {
      buf.update(index, b)
      index += 1
    })
    buf
  }

  /**
    * optimize the conversion manually.
    */
  implicit def toBytes(value: Long, f: Byte => Unit): Unit = {
    f((value >>> 56).toByte)
    f((value >>> 48).toByte)
    f((value >>> 40).toByte)
    f((value >>> 32).toByte)
    f((value >>> 24).toByte)
    f((value >>> 16).toByte)
    f((value >>> 8).toByte)
    f((value >>> 0).toByte)
  }

  /**
    * optimize the conversion manually.
    */
  implicit def toLong(data: Array[Byte], offset: Int = 0): Long = {
    checkSize(SIZE_OF_LONG, data.length - offset)
    var value: Long = 0
    value <<= 8
    value |= data(offset) & 0xFF
    value <<= 8
    value |= data(offset + 1) & 0xFF
    value <<= 8
    value |= data(offset + 2) & 0xFF
    value <<= 8
    value |= data(offset + 3) & 0xFF
    value <<= 8
    value |= data(offset + 4) & 0xFF
    value <<= 8
    value |= data(offset + 5) & 0xFF
    value <<= 8
    value |= data(offset + 6) & 0xFF
    value <<= 8
    value |= data(offset + 7) & 0xFF
    value
  }

  // -------------[int]------------- //
  implicit def toBytes(value: Int): Array[Byte] = {
    val buf = new Array[Byte](SIZE_OF_INT)
    var index = 0
    toBytes(value, (b: Byte) => {
      buf.update(index, b)
      index += 1
    })
    buf
  }

  /**
    * optimize the conversion manually.
    */
  implicit def toBytes(value: Int, f: Byte => Unit): Unit = {
    f((value >>> 24).toByte)
    f((value >>> 16).toByte)
    f((value >>> 8).toByte)
    f((value >>> 0).toByte)
  }

  /**
    * optimize the conversion manually.
    */
  implicit def toInt(data: Array[Byte], offset: Int = 0): Int = {
    checkSize(SIZE_OF_INT, data.length - offset)
    var value: Int = 0
    value <<= 8
    value |= data(offset) & 0xFF
    value <<= 8
    value |= data(offset + 1) & 0xFF
    value <<= 8
    value |= data(offset + 2) & 0xFF
    value <<= 8
    value |= data(offset + 3) & 0xFF
    value
  }

  // -------------[SHORT]------------- //
  implicit def toBytes(value: Short): Array[Byte] = {
    val buf = new Array[Byte](SIZE_OF_SHORT)
    var index = 0
    toBytes(value, (b: Byte) => {
      buf.update(index, b)
      index += 1
    })
    buf
  }

  implicit def toBytes(value: Short, f: Byte => Unit): Unit = {
    f((value >>> 8).toByte)
    f((value >>> 0).toByte)
  }

  implicit def toShort(data: Array[Byte], offset: Int = 0): Short = {
    checkSize(SIZE_OF_SHORT, data.length - offset)
    var value: Short = 0
    value = ((value ^ data(offset)) & 0xFF).toShort
    value = (value << 8).toShort
    value = ((value ^ data(offset + 1)) & 0xFF).toShort
    value
  }

  // -------------[double]------------- //
  implicit def toBytes(value: Double): Array[Byte] = toBytes(java.lang.Double.doubleToRawLongBits(value))

  implicit def toBytes(value: Double, f: Byte => Unit): Unit = toBytes(java.lang.Double.doubleToRawLongBits(value), f)

  implicit def toDouble(data: Array[Byte], offset: Int = 0): Double =
    java.lang.Double.longBitsToDouble(toLong(data, offset))

  // -------------[float]------------- //
  implicit def toBytes(value: Float): Array[Byte] = toBytes(java.lang.Float.floatToIntBits(value))

  implicit def toBytes(value: Float, f: Byte => Unit): Unit = toBytes(java.lang.Float.floatToIntBits(value), f)

  implicit def toFloat(data: Array[Byte], offset: Int = 0): Float = java.lang.Float.intBitsToFloat(toInt(data, offset))

  // -------------[boolean]------------- //
  implicit def toBytes(value: Boolean): Array[Byte] = if (value) Array[Byte]((-1).toByte) else Array[Byte](0.toByte)

  implicit def toBytes(value: Boolean, f: Byte => Unit): Unit = if (value) f((-1).toByte) else f((0).toByte)

  implicit def toBoolean(data: Array[Byte], offset: Int = 0): Boolean = data(offset) != 0

  // -------------[comparator]------------- //
  /**
    * Compare two byte arrays.
    * @param buf1 byte array 1
    * @param buf2 byte array 2
    * @return -1 if array1 is smaller than array2. 0 means they have same content. 1 if array2 is smaller than array1
    */
  def compare(buf1: Array[Byte], buf2: Array[Byte]): Int = compare(buf1, 0, buf1.size, buf2, 0, buf2.size)

  /**
    * Compare a specified range of two byte array
    * @param buf1 byte array 1
    * @param offset1 offset 1
    * @param len1 length 1
    * @param buf2 byte array 2
    * @param offset2 offset 2
    * @param len2 length 2
    * @return -1 if array1 is smaller than array2. 0 means they have same content. 1 if array2 is smaller than array1
    */
  def compare(buf1: Array[Byte], offset1: Int, len1: Int, buf2: Array[Byte], offset2: Int, len2: Int): Int = {
    if (buf1 == buf2 && offset1 == offset2 && len1 == len2) 0
    else {
      val end1 = offset1 + len1
      for (index1 <- offset1 until end1 if (index1 - offset1 < len2)) {
        val index2 = index1 - offset1 + offset2
        val a = buf1(index1) & 0xff
        val b = buf2(index2) & 0xff
        if (a != b) return a - b
      }
      len1 - len2
    }
  }

  // -------------[help method]------------- //
  private[this] def checkSize(expected: Int, actual: Int): Unit = {
    if (expected > actual)
      throw new IllegalArgumentException(s"The bytes size $actual should be bigger then or equal with $expected")
  }
}
