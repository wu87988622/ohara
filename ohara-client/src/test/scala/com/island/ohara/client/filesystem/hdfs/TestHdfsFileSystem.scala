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

package com.island.ohara.client.filesystem.hdfs

import java.io.File

import com.island.ohara.client.filesystem.{FileSystem, FileSystemTestBase}
import com.island.ohara.common.exception.OharaFileSystemException
import com.island.ohara.common.util.CommonUtils
import org.scalatest.Matchers

class TestHdfsFileSystem extends FileSystemTestBase with Matchers {

  private[this] val tempFolder: File = CommonUtils.createTempFolder("local_hdfs")

  private[this] val hdfsURL: String = new File(tempFolder.getAbsolutePath).toURI.toString

  override protected val fileSystem: FileSystem = FileSystem.hdfsBuilder.url(hdfsURL).build

  override protected val rootDir: String = tempFolder.toString

  // override this method because the Local HDFS doesn't support append()
  override def testAppend(): Unit = {
    val file = randomFile()
    fileSystem.create(file).close()

    intercept[OharaFileSystemException] {
      fileSystem.append(file)
    }.getMessage shouldBe "Not supported"
  }

}
