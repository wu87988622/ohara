package com.island.ohara.serialization

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.island.ohara.data.{Cell, Row}
import com.island.ohara.io.CloseOnce.doClose
import com.island.ohara.io.{ByteUtil, DataStreamReader, DataStreamWriter}
import com.island.ohara.serialization.DataType._

object RowSerializer extends Serializer[Row] {

  /**
    * Convert the object to a serializable type
    *
    * @param obj object
    * @return a serializable type
    */
  override def to(obj: Row): Array[Byte] = {
    doClose(new ByteArrayOutputStream()) { output =>
      {
        doClose(new DataStreamWriter(output)) { writer =>
          {
            writer.write(0)
            toV0(obj, writer)
            output.toByteArray
          }
        }
      }
    }
  }

  /**
    * Convert the serialized data to object
    *
    * @param serial serialized data
    * @return object
    */
  override def from(serial: Array[Byte]): Row = {
    doClose(new DataStreamReader(new ByteArrayInputStream(serial))) { reader =>
      {
        reader.readInt() match {
          case 0      => fromV0(reader)
          case v: Int => throw new UnsupportedOperationException(s"unsupported version:$v")
        }
      }
    }
  }

  private[this] def fromV0(reader: DataStreamReader): Row = {
    def readCells: DataStreamReader => Seq[Cell[_]] = (reader: DataStreamReader) => {
      val cellCount = reader.readInt()
      if (cellCount < 0)
        throw new IllegalStateException(s"the number of cell is $cellCount. It should be bigger than zero")
      (0 until cellCount).map(_ => {
        // TODO: we know the size of cell so it is doable to locate the cell at single byte array. by chia
        reader.readInt()
        val name = ByteUtil.toString(reader.forceRead(reader.readShort()))
        DataType.of(reader.readByte()) match {
          case BYTES   => Cell.builder.name(name).build(reader.forceRead(reader.readShort()))
          case BOOLEAN => Cell.builder.name(name).build(ByteUtil.toBoolean(reader.forceRead(reader.readShort())))
          case SHORT   => Cell.builder.name(name).build(ByteUtil.toShort(reader.forceRead(reader.readShort())))
          case INT     => Cell.builder.name(name).build(ByteUtil.toInt(reader.forceRead(reader.readShort())))
          case LONG    => Cell.builder.name(name).build(ByteUtil.toLong(reader.forceRead(reader.readShort())))
          case FLOAT   => Cell.builder.name(name).build(ByteUtil.toFloat(reader.forceRead(reader.readShort())))
          case DOUBLE  => Cell.builder.name(name).build(ByteUtil.toDouble(reader.forceRead(reader.readShort())))
          case STRING  => Cell.builder.name(name).build(ByteUtil.toString(reader.forceRead(reader.readShort())))
          case e: Any  => throw new UnsupportedClassVersionError(s"${e.getClass.getName}")
        }
      })
    }
    def readTags: DataStreamReader => Set[String] = (reader: DataStreamReader) => {
      (0 until reader.readShort()).map(_ => ByteUtil.toString(reader.forceRead(reader.readShort()))).toSet
    }
    Row(readCells(reader), readTags(reader))
  }

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
        case v: Array[Byte] => (BYTES, v)
        case v: Boolean     => (BOOLEAN, ByteUtil.toBytes(v))
        case v: Short       => (SHORT, ByteUtil.toBytes(v))
        case v: Int         => (INT, ByteUtil.toBytes(v))
        case v: Long        => (LONG, ByteUtil.toBytes(v))
        case v: Float       => (FLOAT, ByteUtil.toBytes(v))
        case v: Double      => (DOUBLE, ByteUtil.toBytes(v))
        case v: String      => (STRING, ByteUtil.toBytes(v))
        case v: Any         => throw new UnsupportedClassVersionError(s"class:${v.getClass.getName}")
      }
      val nameBytes = ByteUtil.toBytes(cell.name)
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
  val CELL_OVERHEAD_V0: Int = ByteUtil.SIZE_OF_INT // cell length
  +ByteUtil.SIZE_OF_SHORT // cell name length
  +ByteUtil.SIZE_OF_BYTE // cell value type
  +ByteUtil.SIZE_OF_SHORT // cell value length
}
