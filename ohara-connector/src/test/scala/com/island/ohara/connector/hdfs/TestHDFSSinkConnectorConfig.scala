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

package com.island.ohara.connector.hdfs
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestHDFSSinkConnectorConfig extends SmallTest with Matchers {
  val HDFS_URL_VALUE = "hdfs://test:9000"

  @Test
  def testGetFlushLineCount(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      HDFSSinkConnectorConfig(Map(HDFS_URL -> HDFS_URL_VALUE, FLUSH_LINE_COUNT -> "2000"))

    hdfsSinkConnectorConfig.flushLineCount shouldBe 2000
  }

  @Test
  def testGetFlushLineCountDefaultValue(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      HDFSSinkConnectorConfig(Map(HDFS_URL -> HDFS_URL_VALUE))

    hdfsSinkConnectorConfig.flushLineCount shouldBe 1000
  }

  @Test
  def testGetRotateIntervalMS(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      HDFSSinkConnectorConfig(Map(HDFS_URL -> HDFS_URL_VALUE, ROTATE_INTERVAL_MS -> "120000"))

    hdfsSinkConnectorConfig.rotateIntervalMS shouldBe 120000
  }

  @Test
  def testGetRotateIntervalMSDefaultValue(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      HDFSSinkConnectorConfig(Map(HDFS_URL -> HDFS_URL_VALUE))

    hdfsSinkConnectorConfig.rotateIntervalMS shouldBe 60000
  }

  @Test
  def testGetTmpDir(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      HDFSSinkConnectorConfig(Map(HDFS_URL -> HDFS_URL_VALUE, TMP_DIR -> "/root/tmp"))

    hdfsSinkConnectorConfig.tmpDir shouldBe "/root/tmp"
  }

  @Test
  def testGetTmpDirDefaultValue(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      HDFSSinkConnectorConfig(Map(HDFS_URL -> HDFS_URL_VALUE))

    hdfsSinkConnectorConfig.tmpDir shouldBe "/tmp"
  }

  @Test
  def testGetDataDir(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      HDFSSinkConnectorConfig(Map(HDFS_URL -> HDFS_URL_VALUE, DATA_DIR -> "/root/data"))

    hdfsSinkConnectorConfig.dataDir shouldBe "/root/data"
  }

  @Test
  def testGetDataDirDefaultValue(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      HDFSSinkConnectorConfig(Map(HDFS_URL -> HDFS_URL_VALUE))

    hdfsSinkConnectorConfig.dataDir shouldBe "/data"
  }

  @Test
  def testGetDataFilePrefixName(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      HDFSSinkConnectorConfig(Map(HDFS_URL -> HDFS_URL_VALUE, DATAFILE_PREFIX_NAME -> "datapart123AAA"))

    hdfsSinkConnectorConfig.dataFilePrefixName shouldBe "datapart123AAA"
  }

  @Test
  def testGetDataFilePrefixNameDefaultValue(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      HDFSSinkConnectorConfig(Map(HDFS_URL -> HDFS_URL_VALUE))

    hdfsSinkConnectorConfig.dataFilePrefixName shouldBe "part"
  }

  @Test
  def testGetDataFilePrefixNameException1(): Unit = {
    intercept[IllegalArgumentException] {
      HDFSSinkConnectorConfig(Map(HDFS_URL -> HDFS_URL_VALUE, DATAFILE_PREFIX_NAME -> "datapart123AAA-"))
    }.getMessage shouldBe "The datafile.prefix.name value only a-z or A-Z or 0-9"
  }

  @Test
  def testGetDataFilePrefixNameException2(): Unit = {
    intercept[IllegalArgumentException] {
      HDFSSinkConnectorConfig(Map(HDFS_URL -> HDFS_URL_VALUE, DATAFILE_PREFIX_NAME -> "data-part123-AAA"))
    }.getMessage shouldBe "The datafile.prefix.name value only a-z or A-Z or 0-9"
  }

  @Test
  def testGetDataBufferCount(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      HDFSSinkConnectorConfig(Map(HDFS_URL -> HDFS_URL_VALUE, DATA_BUFFER_COUNT -> "50"))

    hdfsSinkConnectorConfig.dataBufferCount shouldBe 50
  }

  @Test
  def testGetDataBufferCountDefaultValue(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      HDFSSinkConnectorConfig(Map(HDFS_URL -> HDFS_URL_VALUE))

    hdfsSinkConnectorConfig.dataBufferCount shouldBe 100
  }

  @Test
  def testTmpDirSameDataDir(): Unit = {
    intercept[IllegalArgumentException] {
      HDFSSinkConnectorConfig(Map(TMP_DIR -> "/tmp", DATA_DIR -> "/tmp"))
    }
  }
}
