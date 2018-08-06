package com.island.ohara.integration

import java.io.{DataInputStream, OutputStream}

import com.island.ohara.io.CloseOnce._
import com.island.ohara.rule.LargeTest
import org.apache.hadoop.fs.{FileSystem, Path}
import org.junit.Test
import org.scalatest.Matchers

class TestMiniHdfs extends LargeTest with Matchers {

  @Test
  def testHDFSDataNodeWithMultiDataNodes(): Unit = {
    doClose(OharaTestUtil.localHDFS(1)) { testUtil =>
      {
        val fileSystem = testUtil.fileSystem()
        val hdfsTempDir = testUtil.tmpDirectory()
        val tmpFolder: Path = new Path(s"$hdfsTempDir/tmp")
        val tmpFile1: Path = new Path(s"$tmpFolder/tempfile1.txt")
        val tmpFile2: Path = new Path(s"$tmpFolder/tempfile2.txt")
        val text: String = "helloworld"
        val helloBytes: Array[Byte] = text.getBytes()

        //Test create folder to local HDFS
        fileSystem.mkdirs(tmpFolder)
        fileSystem.exists(tmpFolder) shouldBe true

        //Test delete folder to local HDFS
        fileSystem.delete(tmpFolder, true)
        fileSystem.exists(tmpFolder) shouldBe false

        //Test create new file to local HDFS
        fileSystem.exists(tmpFile1) shouldBe false
        fileSystem.createNewFile(tmpFile1)
        fileSystem.exists(tmpFile1) shouldBe true

        //Test write data to local HDFS
        val outputStream: OutputStream = fileSystem.create(tmpFile2, true)
        outputStream.write(helloBytes)
        outputStream.close()
        fileSystem.exists(tmpFile2) shouldBe true

        //Test read data from local HDFS
        val inputStream: DataInputStream = fileSystem.open(tmpFile2)
        val result: StringBuilder = new StringBuilder()
        Stream
          .continually(inputStream.read())
          .takeWhile(_ != -1)
          .foreach(x => {
            result.append(x.toChar)
          })
        result.toString() shouldBe text
        inputStream.close()
      }
    }
  }
}
