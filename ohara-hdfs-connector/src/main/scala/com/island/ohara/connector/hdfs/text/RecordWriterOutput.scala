package com.island.ohara.connector.hdfs.text

import com.island.ohara.common.data.{Column, Row}

/**
  * This abstract to define how to write data to file
  */
abstract class RecordWriterOutput {

  /**
    * The data write to file
    * @param row
    */
  def write(isHeader: Boolean, schema: Seq[Column], row: Row): Unit

  /**
    * close file connection
    */
  def close(): Unit
}
