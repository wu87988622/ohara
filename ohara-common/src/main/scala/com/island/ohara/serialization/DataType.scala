package com.island.ohara.serialization

/**
  * List the supported type in default reader/writer.
  * NOTED: DON'T change the index since it is a part of serialization.
  */
sealed abstract class DataType(val index: Byte) extends Serializable {
  def name: String

  /**
    * customary name of type. see BYTES and INT. default, the alias is equal with name.
    * @return alias name
    */
  def alias: String = name
}

object DataType {

  case object BYTES extends DataType(0) {
    override def name: String = "byte array"
    override def alias: String = "bytes"
  }

  case object BOOLEAN extends DataType(1) {
    override def name: String = "boolean"
  }

  case object BYTE extends DataType(2) {
    override def name: String = "byte"
  }

  case object SHORT extends DataType(3) {
    override def name: String = "short"
  }

  case object INT extends DataType(4) {
    override def name: String = "integer"
    override def alias: String = "int"
  }

  case object LONG extends DataType(5) {
    override def name: String = "long"
  }

  case object FLOAT extends DataType(6) {
    override def name: String = "float"
  }

  case object DOUBLE extends DataType(7) {
    override def name: String = "double"
  }

  case object STRING extends DataType(8) {
    override def name: String = "string"
  }

  case object OBJECT extends DataType(9) {
    override def name: String = "object"
  }

  case object ROW extends DataType(10) {
    override def name: String = "row"
  }

  /**
    * @return a array of all supported data type
    */
  val all: Seq[DataType] = Seq(BYTES, BOOLEAN, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, STRING, OBJECT, ROW)

  /**
    * seek the data type by the index
    * @param index index of data type
    * @return Data type
    */
  def of(index: Byte): DataType = all.find(_.index == index).get

  /**
    * seek the data type by the type name
    * @param name index of data type
    * @return Data type
    */
  def of(name: String): DataType =
    all.find(dataType => dataType.name.equalsIgnoreCase(name) || dataType.alias.equalsIgnoreCase(name)).get
}
