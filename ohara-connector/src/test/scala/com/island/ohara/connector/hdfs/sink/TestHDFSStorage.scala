/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.connector.hdfs.sink

import java.io.{InputStream, OutputStream}

import com.island.ohara.kafka.connector.storage.Storage
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.testing.WithTestUtils
import org.apache.hadoop.fs.{FileSystem, Path}
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.JavaConverters._

class TestHDFSStorage extends WithTestUtils with Matchers {

  @Test
  def testHdfsStorage(): Unit = {
    val fileSystem: FileSystem = testUtil.hdfs.fileSystem
    val hdfsTempDir: String = s"${testUtil.hdfs.tmpDirectory}/${CommonUtils.randomString(10)}"
    val hdfsStorage: Storage = new HDFSStorage(fileSystem)
    hdfsStorage.list(hdfsTempDir).asScala.size shouldBe 0

    fileSystem.createNewFile(new Path(s"$hdfsTempDir/file.txt"))
    hdfsStorage.list(hdfsTempDir).asScala.size shouldBe 1

    fileSystem.mkdirs(new Path(s"$hdfsTempDir/1"))
    fileSystem.mkdirs(new Path(s"$hdfsTempDir/2"))
    hdfsStorage.list(hdfsTempDir).asScala.size shouldBe 3
  }

  @Test
  def testCreateFile(): Unit = {
    val fileSystem: FileSystem = testUtil.hdfs.fileSystem
    val hdfsTempDir: String = testUtil.hdfs.tmpDirectory
    val fileName: String = s"$hdfsTempDir/file.txt"
    val hdfsStorage: Storage = new HDFSStorage(fileSystem)
    val text: String = "helloworld"

    fileSystem.createNewFile(new Path(fileName))
    val outputStream: OutputStream = hdfsStorage.create(fileName, true)
    outputStream.write(text.getBytes)
    outputStream.close()

    val inputStream: InputStream = fileSystem.open(new Path(fileName))
    val result: StringBuilder = new StringBuilder()
    Stream
      .continually(inputStream.read())
      .takeWhile(_ != -1)
      .foreach(x => {
        result.append(x.toChar)
      })
    inputStream.close()
    result.toString shouldBe text
  }

  @Test
  def testRename(): Unit = {
    val fileSystem: FileSystem = testUtil.hdfs.fileSystem
    val hdfsTempDir: String = testUtil.hdfs.tmpDirectory
    val folderName: String = s"$hdfsTempDir/folder1"
    val newFolderName: String = s"$hdfsTempDir/folder2"

    fileSystem.create(new Path(folderName))

    val hdfsStorage: Storage = new HDFSStorage(fileSystem)
    hdfsStorage.exists(folderName) shouldBe true
    hdfsStorage.move(folderName, newFolderName) shouldBe true
    hdfsStorage.exists(folderName) shouldBe false
    hdfsStorage.exists(newFolderName) shouldBe true
  }
}
