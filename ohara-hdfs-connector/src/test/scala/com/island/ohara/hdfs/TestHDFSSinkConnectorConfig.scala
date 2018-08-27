package com.island.ohara.hdfs

import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestHDFSSinkConnectorConfig extends SmallTest with Matchers {
  val HDFS_URL_VALUE = "hdfs://test:9000"
  @Test
  def testGetFlushLineCount(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(
        Map(HDFSSinkConnectorConfig.HDFS_URL -> HDFS_URL_VALUE, HDFSSinkConnectorConfig.FLUSH_LINE_COUNT -> "2000"))

    hdfsSinkConnectorConfig.flushLineCount() shouldBe 2000
  }

  @Test
  def testGetFlushLineCountDefaultValue(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(Map(HDFSSinkConnectorConfig.HDFS_URL -> HDFS_URL_VALUE))

    hdfsSinkConnectorConfig.flushLineCount() shouldBe 1000
  }

  @Test
  def testGetRotateIntervalMS(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(
        Map(HDFSSinkConnectorConfig.HDFS_URL -> HDFS_URL_VALUE, HDFSSinkConnectorConfig.ROTATE_INTERVAL_MS -> "120000"))

    hdfsSinkConnectorConfig.rotateIntervalMS() shouldBe 120000
  }

  @Test
  def testGetRotateIntervalMSDefaultValue(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(Map(HDFSSinkConnectorConfig.HDFS_URL -> HDFS_URL_VALUE))

    hdfsSinkConnectorConfig.rotateIntervalMS() shouldBe 60000
  }

  @Test
  def testGetTmpDir(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(
        Map(HDFSSinkConnectorConfig.HDFS_URL -> HDFS_URL_VALUE, HDFSSinkConnectorConfig.TMP_DIR -> "/root/tmp"))

    hdfsSinkConnectorConfig.tmpDir() shouldBe "/root/tmp"
  }

  @Test
  def testGetTmpDirDefaultValue(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(Map(HDFSSinkConnectorConfig.HDFS_URL -> HDFS_URL_VALUE))

    hdfsSinkConnectorConfig.tmpDir() shouldBe "/tmp"
  }

  @Test
  def testGetOffsetDir(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(Map(HDFSSinkConnectorConfig.OFFSET_DIR -> "/root/offset"))

    hdfsSinkConnectorConfig.offsetDir() shouldBe "/root/offset"
  }

  @Test
  def testGetOffsetDirDefaultValue(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(Map(HDFSSinkConnectorConfig.HDFS_URL -> HDFS_URL_VALUE))

    hdfsSinkConnectorConfig.offsetDir() shouldBe "/offset"
  }

  @Test
  def testGetDataDir(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(
        Map(HDFSSinkConnectorConfig.HDFS_URL -> HDFS_URL_VALUE, HDFSSinkConnectorConfig.DATA_DIR -> "/root/data"))

    hdfsSinkConnectorConfig.dataDir() shouldBe "/root/data"
  }

  @Test
  def testGetDataDirDefaultValue(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(Map(HDFSSinkConnectorConfig.HDFS_URL -> HDFS_URL_VALUE))

    hdfsSinkConnectorConfig.dataDir() shouldBe "/data"
  }

  @Test
  def testGetDataFilePrefixName(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(
        Map(HDFSSinkConnectorConfig.HDFS_URL -> HDFS_URL_VALUE,
            HDFSSinkConnectorConfig.DATAFILE_PREFIX_NAME -> "datapart123AAA"))

    hdfsSinkConnectorConfig.dataFilePrefixName() shouldBe "datapart123AAA"
  }

  @Test
  def testGetDataFilePrefixNameDefaultValue(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(Map(HDFSSinkConnectorConfig.HDFS_URL -> HDFS_URL_VALUE))

    hdfsSinkConnectorConfig.dataFilePrefixName() shouldBe "part"
  }

  @Test
  def testGetDataFilePrefixNameException1(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(
        Map(HDFSSinkConnectorConfig.HDFS_URL -> HDFS_URL_VALUE,
            HDFSSinkConnectorConfig.DATAFILE_PREFIX_NAME -> "datapart123AAA"))

    assertThrows[RuntimeException] {
      hdfsSinkConnectorConfig.dataFilePrefixName() shouldBe "datapart123AAA-"
    }
  }

  @Test
  def testGetDataFilePrefixNameException2(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(
        Map(HDFSSinkConnectorConfig.HDFS_URL -> HDFS_URL_VALUE,
            HDFSSinkConnectorConfig.DATAFILE_PREFIX_NAME -> "data-part123-AAA"))

    assertThrows[RuntimeException] {
      hdfsSinkConnectorConfig.dataFilePrefixName() shouldBe "data-part123-AAA"
    }
  }

  @Test
  def testGetOffsetInconsistentSkip(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(
        Map(HDFSSinkConnectorConfig.HDFS_URL -> HDFS_URL_VALUE,
            HDFSSinkConnectorConfig.OFFSET_INCONSISTENT_SKIP -> "true"))

    hdfsSinkConnectorConfig.offsetInconsistentSkip() shouldBe true
  }

  @Test
  def testGetOffsetInconsistentSkipDefaultValue(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(Map(HDFSSinkConnectorConfig.HDFS_URL -> HDFS_URL_VALUE))

    hdfsSinkConnectorConfig.offsetInconsistentSkip() shouldBe false
  }

  @Test
  def testGetDataBufferCount(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(
        Map(HDFSSinkConnectorConfig.HDFS_URL -> HDFS_URL_VALUE, HDFSSinkConnectorConfig.DATA_BUFFER_COUNT -> "50"))

    hdfsSinkConnectorConfig.dataBufferCount() shouldBe 50
  }

  @Test
  def testGetDataBufferCountDefaultValue(): Unit = {
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(Map(HDFSSinkConnectorConfig.HDFS_URL -> HDFS_URL_VALUE))

    hdfsSinkConnectorConfig.dataBufferCount() shouldBe 100
  }

}
