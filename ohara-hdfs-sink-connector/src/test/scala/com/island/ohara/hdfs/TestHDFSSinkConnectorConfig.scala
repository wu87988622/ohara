package com.island.ohara.hdfs

import java.util
import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestHDFSSinkConnectorConfig extends SmallTest with Matchers {
  val HDFS_URL_VALUE = "hdfs://test:9000"
  @Test
  def testGetFlushLineCount(): Unit = {
    var props: util.Map[String, String] = new util.HashMap[String, String]
    props.put(HDFSSinkConnectorConfig.HDFS_URL, HDFS_URL_VALUE)
    props.put(HDFSSinkConnectorConfig.FLUSH_LINE_COUNT, "2000")

    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(props)

    hdfsSinkConnectorConfig.flushLineCount() shouldBe 2000
  }

  @Test
  def testGetFlushLineCountDefaultValue(): Unit = {
    val props: util.Map[String, String] = new util.HashMap[String, String]
    props.put(HDFSSinkConnectorConfig.HDFS_URL, HDFS_URL_VALUE)

    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(props)

    hdfsSinkConnectorConfig.flushLineCount() shouldBe 1000
  }

  @Test
  def testGetRotateIntervalMS(): Unit = {
    val props: util.Map[String, String] = new util.HashMap[String, String]
    props.put(HDFSSinkConnectorConfig.HDFS_URL, HDFS_URL_VALUE)
    props.put(HDFSSinkConnectorConfig.ROTATE_INTERVAL_MS, "120000")

    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(props)

    hdfsSinkConnectorConfig.rotateIntervalMS() shouldBe 120000
  }

  @Test
  def testGetRotateIntervalMSDefaultValue(): Unit = {
    val props: util.Map[String, String] = new util.HashMap[String, String]
    props.put(HDFSSinkConnectorConfig.HDFS_URL, HDFS_URL_VALUE)

    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(props)

    hdfsSinkConnectorConfig.rotateIntervalMS() shouldBe 60000
  }

  @Test
  def testGetTmpDir(): Unit = {
    val props: util.Map[String, String] = new util.HashMap[String, String]
    props.put(HDFSSinkConnectorConfig.HDFS_URL, HDFS_URL_VALUE)
    props.put(HDFSSinkConnectorConfig.TMP_DIR, "/root/tmp")

    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(props)

    hdfsSinkConnectorConfig.tmpDir() shouldBe "/root/tmp"
  }

  @Test
  def testGetTmpDirDefaultValue(): Unit = {
    val props: util.Map[String, String] = new util.HashMap[String, String]
    props.put(HDFSSinkConnectorConfig.HDFS_URL, HDFS_URL_VALUE)

    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(props)

    hdfsSinkConnectorConfig.tmpDir() shouldBe "/tmp"
  }

  @Test
  def testGetOffsetDir(): Unit = {
    val props: util.Map[String, String] = new util.HashMap[String, String]
    props.put(HDFSSinkConnectorConfig.HDFS_URL, HDFS_URL_VALUE)
    props.put(HDFSSinkConnectorConfig.OFFSET_DIR, "/root/offset")

    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(props)

    hdfsSinkConnectorConfig.offsetDir() shouldBe "/root/offset"
  }

  @Test
  def testGetOffsetDirDefaultValue(): Unit = {
    val props: util.Map[String, String] = new util.HashMap[String, String]
    props.put(HDFSSinkConnectorConfig.HDFS_URL, HDFS_URL_VALUE)

    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(props)

    hdfsSinkConnectorConfig.offsetDir() shouldBe "/offset"
  }

  @Test
  def testGetDataDir(): Unit = {
    val props: util.Map[String, String] = new util.HashMap[String, String]
    props.put(HDFSSinkConnectorConfig.HDFS_URL, HDFS_URL_VALUE)
    props.put(HDFSSinkConnectorConfig.DATA_DIR, "/root/data")

    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(props)

    hdfsSinkConnectorConfig.dataDir() shouldBe "/root/data"
  }

  @Test
  def testGetDataDirDefaultValue(): Unit = {
    val props: util.Map[String, String] = new util.HashMap[String, String]
    props.put(HDFSSinkConnectorConfig.HDFS_URL, HDFS_URL_VALUE)

    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(props)

    hdfsSinkConnectorConfig.dataDir() shouldBe "/data"
  }

  @Test
  def testGetDataFilePrefixName(): Unit = {
    val props: util.Map[String, String] = new util.HashMap[String, String]
    props.put(HDFSSinkConnectorConfig.HDFS_URL, HDFS_URL_VALUE)
    props.put(HDFSSinkConnectorConfig.DATAFILE_PREFIX_NAME, "datapart")

    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(props)

    hdfsSinkConnectorConfig.dataFilePrefixName() shouldBe "datapart"
  }

  @Test
  def testGetDataFilePrefixNameDefaultValue(): Unit = {
    val props: util.Map[String, String] = new util.HashMap[String, String]
    props.put(HDFSSinkConnectorConfig.HDFS_URL, HDFS_URL_VALUE)

    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(props)

    hdfsSinkConnectorConfig.dataFilePrefixName() shouldBe "part-"
  }

  @Test
  def testGetOffsetInconsistentSkip(): Unit = {
    val props: util.Map[String, String] = new util.HashMap[String, String]
    props.put(HDFSSinkConnectorConfig.HDFS_URL, HDFS_URL_VALUE)
    props.put(HDFSSinkConnectorConfig.OFFSET_INCONSISTENT_SKIP, "true")

    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(props)

    hdfsSinkConnectorConfig.offsetInconsistentSkip() shouldBe true
  }

  @Test
  def testGetOffsetInconsistentSkipDefaultValue(): Unit = {
    val props: util.Map[String, String] = new util.HashMap[String, String]
    props.put(HDFSSinkConnectorConfig.HDFS_URL, HDFS_URL_VALUE)

    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(props)

    hdfsSinkConnectorConfig.offsetInconsistentSkip() shouldBe false
  }

  @Test
  def testGetDataBufferCount(): Unit = {
    val props: util.Map[String, String] = new util.HashMap[String, String]
    props.put(HDFSSinkConnectorConfig.HDFS_URL, HDFS_URL_VALUE)
    props.put(HDFSSinkConnectorConfig.DATA_BUFFER_COUNT, "50")

    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(props)

    hdfsSinkConnectorConfig.dataBufferCount() shouldBe 50
  }

  @Test
  def testGetDataBufferCountDefaultValue(): Unit = {
    val props: util.Map[String, String] = new util.HashMap[String, String]
    props.put(HDFSSinkConnectorConfig.HDFS_URL, HDFS_URL_VALUE)

    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig =
      new HDFSSinkConnectorConfig(props)

    hdfsSinkConnectorConfig.dataBufferCount() shouldBe 100
  }

}
