package com.island.ohara.serialization

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.island.ohara.data.{Cell, Row}
import com.island.ohara.io.CloseOnce._
import com.island.ohara.io.{ByteUtil, DataStreamReader, DataStreamWriter}
import com.island.ohara.serialization.DataType._

object RowSerializer extends Serializer[Row] {

  /**
    * Convert the object to a serializable type
    *
    * @param obj object
    * @return a serializable type
    */
  override def to(obj: Row): Array[Byte] = doClose2(new ByteArrayOutputStream())(new DataStreamWriter(_)) {
    case (buf, writer) =>
      writer.write(0)
      toV0(obj, writer)
      buf.toByteArray
  }

  /**
    * Convert the serialized data to object
    *
    * @param serial serialized data
    * @return object
    */
  override def from(serial: Array[Byte]): Row = {
    doClose(new DataStreamReader(new ByteArrayInputStream(serial))) { reader =>
      reader.readInt() match {
        case 0      => fromV0(reader)
        case v: Int => throw new UnsupportedOperationException(s"unsupported version:$v")
      }
    }
  }

  private[this] def fromV0(reader: DataStreamReader): Row = Row
    .builder()
    // NOTED: DON'T change the order of this call chain. otherwise, the serialization will be broken
    .cells {
      val cellCount = reader.readInt()
      if (cellCount < 0)
        throw new IllegalStateException(s"the number of cell is $cellCount. It should be bigger than zero")
      (0 until cellCount).map(_ => {
        // TODO: we know the size of cell so it is doable to locate the cell at single byte array. by chia
        reader.readInt()
        val name = ByteUtil.toString(reader.forceRead(reader.readShort()))
        val cell = DataType.of(reader.readByte()) match {
          case BYTES   => Cell(name, reader.forceRead(reader.readShort()))
          case BOOLEAN => Cell(name, Serializer.BOOLEAN.from(reader.forceRead(reader.readShort())))
          case SHORT   => Cell(name, Serializer.SHORT.from(reader.forceRead(reader.readShort())))
          case INT     => Cell(name, Serializer.INT.from(reader.forceRead(reader.readShort())))
          case LONG    => Cell(name, Serializer.LONG.from(reader.forceRead(reader.readShort())))
          case FLOAT   => Cell(name, Serializer.FLOAT.from(reader.forceRead(reader.readShort())))
          case DOUBLE  => Cell(name, Serializer.DOUBLE.from(reader.forceRead(reader.readShort())))
          case STRING  => Cell(name, Serializer.STRING.from(reader.forceRead(reader.readShort())))
          case OBJECT  => Cell(name, Serializer.OBJECT.from(reader.forceRead(reader.readShort())))
          case ROW     => Cell(name, Serializer.ROW.from(reader.forceRead(reader.readShort())))
          case e: Any  => throw new UnsupportedClassVersionError(s"${e.getClass.getName}")
        }
        cell
      })
    }
    .tags((0 until reader.readShort()).map(_ => ByteUtil.toString(reader.forceRead(reader.readShort()))).toSet)
    .build()

  /**
    * cell count of row (4 bytes)
    * cell length (4 bytes) | cell name length (2 bytes) | cell name | cell value type (1 byte) | cell value length (2 bytes) | cell value
    * cell length (4 bytes) | cell name length (2 bytes) | cell name | cell value type (1 byte) | cell value length (2 bytes) | cell value
    * tag count (2 bytes)
    * tag length (2 bytes) | tag bytes
    * tag length (2 bytes) | tag bytes
    *
    * @param row row
    * @param writer writer
    * @return byte array
    */
  private[this] def toV0(row: Row, writer: DataStreamWriter): Unit = {
    writer.write(row.size)
    row.foreach(cell => {
      val (valueType, valueBytes) = cell.value match {
        case v: Array[Byte]          => (BYTES, v)
        case v: Boolean              => (BOOLEAN, Serializer.BOOLEAN.to(v))
        case v: Short                => (SHORT, Serializer.SHORT.to(v))
        case v: Int                  => (INT, Serializer.INT.to(v))
        case v: Long                 => (LONG, Serializer.LONG.to(v))
        case v: Float                => (FLOAT, Serializer.FLOAT.to(v))
        case v: Double               => (DOUBLE, Serializer.DOUBLE.to(v))
        case v: String               => (STRING, Serializer.STRING.to(v))
        case v: java.io.Serializable => (OBJECT, Serializer.OBJECT.to(v))
        case v: Row                  => (ROW, Serializer.ROW.to(v))
        case v: Any                  => throw new UnsupportedClassVersionError(s"class:${v.getClass.getName}")
      }
      if (valueBytes.length > Short.MaxValue)
        throw new IllegalArgumentException(s"the max size of value is ${Short.MaxValue}. current:${valueBytes.length}")
      val nameBytes = ByteUtil.toBytes(cell.name)
      if (nameBytes.length > Short.MaxValue)
        throw new IllegalArgumentException(s"the max size of name is ${Short.MaxValue}. current:${nameBytes.length}")
      val cellSize = CELL_OVERHEAD_V0 + nameBytes.length + valueBytes.length
      writer.write(cellSize)
      // convert the int to short
      writer.write(nameBytes.length.toShort)
      writer.write(nameBytes)
      writer.write(valueType.index)
      // convert the int to short
      writer.write(valueBytes.length.toShort)
      writer.write(valueBytes)
    })
    // convert the int to short
    writer.write(row.tags.size.toShort)
    row.tags.foreach(tag => {
      val bytes = ByteUtil.toBytes(tag)
      // convert the int to short
      writer.write(bytes.length.toShort)
      writer.write(bytes)
    })
  }

  /**
    *  cell length + cell name length + cell value type +cell value length
    */
  val CELL_OVERHEAD_V0
    : Int = ByteUtil.SIZE_OF_INT + ByteUtil.SIZE_OF_SHORT + ByteUtil.SIZE_OF_BYTE + ByteUtil.SIZE_OF_SHORT
}
