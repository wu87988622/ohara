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
private class LocalHDFS(numDataNodes: Int) extends CloseOnce {

  private[this] val tmpDir: File = createTempDir("hdfs-local")

  /**
    * Creating the FileSystem object is expensive.
    * The FileSystem returned to user should be a same object in test.
    *
    * @return Get to Local FileSystem
    */
  def fileSystem(): FileSystem = {
    val config: Configuration = new Configuration()
    FileSystem.getLocal(config)
  }

  /**
    * @return Get to tmp dir path
    */
  def tmpDirPath(): String = tmpDir.getPath()

  override protected def doClose(): Unit = {
    fileSystem.close()
    deleteFile(tmpDir)
  }
}
