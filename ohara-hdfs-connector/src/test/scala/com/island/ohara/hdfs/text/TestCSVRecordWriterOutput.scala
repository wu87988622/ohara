package com.island.ohara.hdfs.text

import java.io.{InputStream, OutputStream}
import java.util

import com.island.ohara.core.{Cell, Row}
import com.island.ohara.hdfs.{HDFSSinkConnectorConfig, HadoopConfigurationConstants}
import com.island.ohara.hdfs.storage.{HDFSStorage, Storage}
import com.island.ohara.integration.{LocalHDFS, OharaTestUtil}
import com.island.ohara.rule.MediumTest
import org.junit.Test
import org.scalatest.Matchers
import com.island.ohara.io.CloseOnce._
import org.apache.hadoop.fs.{FileSystem, Path}

class TestCSVRecordWriterOutput extends MediumTest with Matchers {

  @Test
  def testWriteData(): Unit = {
    val localHDFS: LocalHDFS = OharaTestUtil.localHDFS(1)
    val props: util.HashMap[String, String] = new util.HashMap[String, String]()
    props.put(HDFSSinkConnectorConfig.HDFS_URL, localHDFS.tmpDirPath)

    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig = new HDFSSinkConnectorConfig(props)
    val fileSystem: FileSystem = localHDFS.fileSystem()
    val storage: Storage = new HDFSStorage(fileSystem)
    val tempFilePath: String = s"${localHDFS.tmpDirPath}/file1.txt"
    val csvRecordWriter: RecordWriterOutput =
      new CSVRecordWriterOutput(hdfsSinkConnectorConfig, storage, tempFilePath)

    val row: Row = Row(Cell.builder.name("column1").build("value1"), Cell.builder.name("column2").build("value2"))
    csvRecordWriter.write(row)
    csvRecordWriter.close()
    storage.exists(tempFilePath) shouldBe true

    doClose(fileSystem.open(new Path(tempFilePath))) { inputStream =>
      {
        var buffer: Array[Byte] = Array[Byte]()
        var result: StringBuilder = new StringBuilder()
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
