package com.island.ohara.hdfs.text

import com.island.ohara.data.Row

/**
  * This abstract to define how to write data to file
  */
abstract class RecordWriterOutput {

  /**
    * The data write to file
    * @param row
    */
  def write(row: Row): Unit

  /**
    * close file connection
    */
  def close(): Unit
}
