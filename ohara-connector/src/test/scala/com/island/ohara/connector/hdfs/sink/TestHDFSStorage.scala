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

import java.io.IOException
import java.nio.file.{Path, Paths}

import com.island.ohara.kafka.connector.storage.StorageTestBase
import com.island.ohara.common.util.CommonUtils
import org.scalatest.Matchers

class TestHDFSStorage extends StorageTestBase with Matchers {
  override protected def createStorage() = new HDFSStorage(testUtil.hdfs.fileSystem)

  override protected def getRootFolder: Path =
    Paths.get(CommonUtils.path(testUtil.hdfs.tmpDirectory, CommonUtils.randomString(10)))

  // override this method because the Local HDFS doesn't support append()
  override def testAppend(): Unit = {
    val file = randomFile()
    getStorage.create(file).close()

    intercept[IOException] {
      getStorage.append(file)
    }.getMessage shouldBe "Not supported"
  }
}
