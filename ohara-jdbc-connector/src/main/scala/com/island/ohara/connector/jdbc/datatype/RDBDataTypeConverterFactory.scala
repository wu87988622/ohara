package com.island.ohara.connector.jdbc.datatype

object RDBDataTypeConverterFactory {

  def dataTypeConverter(): RDBDataTypeConverter = {
    new RDBDataTypeConverter()
  }
}
