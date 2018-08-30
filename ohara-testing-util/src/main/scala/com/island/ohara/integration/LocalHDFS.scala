package com.island.ohara.integration

import java.io.File

import com.island.ohara.io.CloseOnce
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem

/**
  * This class for HDFS Test
  *
  * @param numDataNodes
  */
//TODO This class haven't been completed. Perhaps we should make it be pseudo dist in 0.2
class LocalHDFS private[integration] (numDataNodes: Int) extends CloseOnce {

  private[this] val tmpDir: File = createTempDir("hdfs-local")

  /**
    * Creating the FileSystem object is expensive.
    * The FileSystem returned to user should be a same object in test.
    */
  val fs: FileSystem = FileSystem.newInstance(new Configuration())

  /**
    * @return Get to tmp dir path
    */
  def tmpDirectory: String = tmpDir.getPath

  override protected def doClose(): Unit = {
    deleteFile(tmpDir)
  }
}

object LocalHDFS {
  def apply(numDataNodes: Int): LocalHDFS = {
    new LocalHDFS(numDataNodes)
  }
}
