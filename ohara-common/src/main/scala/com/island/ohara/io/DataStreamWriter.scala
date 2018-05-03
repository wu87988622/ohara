package com.island.ohara.io

import java.io.OutputStream

class DataStreamWriter(val output: OutputStream) extends AutoCloseable {

  def write(v: Byte): DataStreamWriter = {
    output.write(v)
    this
  }

  def write(v: Array[Byte]): DataStreamWriter = {
    output.write(v)
    this
  }

  def write(v: Boolean): DataStreamWriter = {
    output.write(ByteUtil.toBytes(v))
    this
  }

  def write(v: Short): DataStreamWriter = {
    output.write(ByteUtil.toBytes(v))
    this
  }

  def write(v: Int): DataStreamWriter = {
    output.write(ByteUtil.toBytes(v))
    this
  }

  def write(v: Long): DataStreamWriter = {
    output.write(ByteUtil.toBytes(v))
    this
  }

  def write(v: Float): DataStreamWriter = {
    output.write(ByteUtil.toBytes(v))
    this
  }

  def write(v: Double): DataStreamWriter = {
    output.write(ByteUtil.toBytes(v))
    this
  }

  def write(v: String): DataStreamWriter = {
    output.write(ByteUtil.toBytes(v))
    this
  }

  override def close() = output.close()

}

object DataStreamWriter {
  def apply(output: OutputStream) = new DataStreamWriter(output)
}
