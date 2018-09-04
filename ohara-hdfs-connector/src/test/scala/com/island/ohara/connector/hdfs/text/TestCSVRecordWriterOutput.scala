package com.island.ohara.connector.hdfs.text

import com.island.ohara.connector.hdfs.HDFSSinkConnectorConfig
import com.island.ohara.connector.hdfs.storage.{HDFSStorage, Storage}
import com.island.ohara.data.{Cell, Row}
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.CloseOnce._
import com.island.ohara.rule.MediumTest
import org.apache.hadoop.fs.{FileSystem, Path}
import org.junit.Test
import org.scalatest.Matchers

class TestCSVRecordWriterOutput extends MediumTest with Matchers {

  @Test
  def testWriteData(): Unit = {
    val testUtil = OharaTestUtil.localHDFS(1)
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig = new HDFSSinkConnectorConfig(
      Map(HDFSSinkConnectorConfig.HDFS_URL -> testUtil.tmpDirectory))
    val fileSystem: FileSystem = testUtil.fileSystem
    val storage: Storage = new HDFSStorage(fileSystem)
    val tempFilePath: String = s"${testUtil.tmpDirectory}/file1.txt"
    val csvRecordWriter: RecordWriterOutput =
      new CSVRecordWriterOutput(storage, tempFilePath)

    val row: Row = Row(Seq(Cell.builder.name("column1").build("value1"), Cell.builder.name("column2").build("value2")))
    csvRecordWriter.write(row)
    csvRecordWriter.close()
    storage.exists(tempFilePath) shouldBe true

    doClose(fileSystem.open(new Path(tempFilePath))) { inputStream =>
      {
        val result: StringBuilder = new StringBuilder()
        Stream
          .continually(inputStream.read())
          .takeWhile(_ != -1)
          .foreach(x => {
            result.append(x.toChar)
          })
        result.toString shouldBe "value1,value2\n"
      }
    }
  }
}
