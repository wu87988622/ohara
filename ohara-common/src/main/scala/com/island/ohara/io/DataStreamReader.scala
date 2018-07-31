package com.island.ohara.io

import java.io.InputStream

class DataStreamReader(input: InputStream) extends AutoCloseable {
  def readByte(): Byte = input.read().toByte

  def readBytes(buf: Array[Byte]): Array[Byte] = readBytes(buf, 0, buf.length)

  def readBytes(buf: Array[Byte], offset: Int, length: Int): Array[Byte] = {
    input.read(buf, offset, length)
    buf

  }

  def forceRead(length: Int): Array[Byte] = {
    if (length <= 0) throw new IllegalStateException(s"length:$length should be bigger than zero")
    var remaining = length
    val buf = new Array[Byte](length)
    while (remaining != 0) {
      val rval = input.read(buf, buf.length - remaining, remaining)
      if (rval < 0) throw new IllegalStateException(s"required $length bytes but actual ${length - remaining} bytes")
      else {
        if (rval > remaining) throw new IllegalStateException(s"ask $remaining bytes but actual $rval bytes")
        remaining -= rval
        if (remaining == 0) return buf
      }
    }
    throw new IllegalStateException("you are at the end of world...")
  }

  def readShort(): Short = ByteUtil.toShort(forceRead(ByteUtil.SIZE_OF_SHORT))

  def readInt(): Int = ByteUtil.toInt(forceRead(ByteUtil.SIZE_OF_INT))

  def readLong(): Long = ByteUtil.toLong(forceRead(ByteUtil.SIZE_OF_LONG))

  def readFloat(): Float = ByteUtil.toFloat(forceRead(ByteUtil.SIZE_OF_FLOAT))

  def readDouble(): Double = ByteUtil.toDouble(forceRead(ByteUtil.SIZE_OF_DOUBLE))

  def readString(length: Int): String = ByteUtil.toString(forceRead(length))

  override def close() = input.close()
}
