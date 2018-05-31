package com.island.ohara.hdfs

import java.util
import java.util.concurrent.ConcurrentHashMap

import com.island.ohara.core.{Cell, Row}
import com.island.ohara.hdfs.creator.LocalHDFSStorageCreator
import com.island.ohara.hdfs.storage.HDFSStorage
import com.island.ohara.integration._
import com.island.ohara.io.ByteUtil
import com.island.ohara.rule.MediumTest
import com.island.ohara.io.CloseOnce._
import com.island.ohara.kafka.RowProducer
import com.island.ohara.kafka.connector.RowSinkTask
import org.apache.hadoop.fs.Path
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestHDFSSinkConnector extends MediumTest with Matchers {
  val hdfsURL: String = "hdfs://host1:9000"
  val tmpDir: String = "/tmp"

  @Test
  def testTaskConfigs(): Unit = {
    val maxTasks = 5
    val hdfsSinkConnector = new HDFSSinkConnector()
    val props: util.Map[String, String] = new util.HashMap[String, String]()
    props.put(HDFSSinkConnectorConfig.HDFS_URL, hdfsURL)
    props.put(HDFSSinkConnectorConfig.TMP_DIR, tmpDir)

    hdfsSinkConnector.start(props)
    val result: util.List[util.Map[String, String]] = hdfsSinkConnector.taskConfigs(maxTasks)

    result.size() shouldBe maxTasks
    result.get(0).get(HDFSSinkConnectorConfig.HDFS_URL) shouldBe hdfsURL
    result.get(0).get(HDFSSinkConnectorConfig.TMP_DIR) shouldBe tmpDir
  }

  @Test
  def testRunMiniClusterAndAssignConfig(): Unit = {
    val sinkTasks = 2
    val flushLineCountName = HDFSSinkConnectorConfig.FLUSH_LINE_COUNT
    val flushLineCount = "2000"
    val rotateIntervalMSName = HDFSSinkConnectorConfig.ROTATE_INTERVAL_MS
    val tmpDirName = HDFSSinkConnectorConfig.TMP_DIR
    val tmpDirPath = "/home/tmp"
    val hdfsURLName = HDFSSinkConnectorConfig.HDFS_URL

    doClose(new OharaTestUtil(3, 1)) { testUtil =>
      val localURL = s"file://${testUtil.hdfsTempDir()}"
      testUtil
        .sinkConnectorCreator()
        .name("my_sink_connector")
        .connectorClass(classOf[SimpleHDFSSinkConnector])
        .topic("my_connector_topic")
        .taskNumber(sinkTasks)
        .disableConverter
        .config(Map(flushLineCountName -> flushLineCount, tmpDirName -> tmpDirPath, hdfsURLName -> localURL))
        .run()

      testUtil.await(() => SimpleHDFSSinkTask.taskProps.get("topics") == "my_connector_topic", 20 second)
      testUtil.await(() => SimpleHDFSSinkTask.taskProps.get(flushLineCountName) == flushLineCount, 20 second)
      testUtil.await(() => SimpleHDFSSinkTask.taskProps.get(rotateIntervalMSName) == null, 20 second)
      testUtil.await(() => SimpleHDFSSinkTask.taskProps.get(tmpDirName) == tmpDirPath, 10 second)
      testUtil.await(() => SimpleHDFSSinkTask.sinkConnectorConfig.dataDir() == "/data", 20 second)
      testUtil.await(() => SimpleHDFSSinkTask.sinkConnectorConfig.flushLineCount() == 2000, 20 second)
      testUtil.await(() => SimpleHDFSSinkTask.sinkConnectorConfig.offsetInconsistentSkip() == false, 20 second)
    }
  }

  @Test
  def testHDFSSinkConnector(): Unit = {
    val sinkTasks = 1
    val flushLineCountName = HDFSSinkConnectorConfig.FLUSH_LINE_COUNT
    val flushLineCount = "10"
    val rotateIntervalMSName = HDFSSinkConnectorConfig.ROTATE_INTERVAL_MS
    val tmpDirName = HDFSSinkConnectorConfig.TMP_DIR
    val dataDirName = HDFSSinkConnectorConfig.DATA_DIR
    val hdfsURLName = HDFSSinkConnectorConfig.HDFS_URL
    val topicName = "topic1"
    val rowCount = 100
    val row = Row.builder.append(Cell.builder.name("cf0").build(10)).append(Cell.builder.name("cf1").build(11)).build
    val hdfsCreatorClassName = HDFSSinkConnectorConfig.HDFS_STORAGE_CREATOR_CLASS
    val hdfsCreatorClassNameValue = classOf[LocalHDFSStorageCreator].getName()

    doClose(new OharaTestUtil(3, 1)) { testUtil =>
      val fileSystem = testUtil.hdfsFileSystem()
      val storage = new HDFSStorage(fileSystem)
      val tmpDirPath = s"${testUtil.hdfsTempDir}/tmp"
      val dataDirPath = s"${testUtil.hdfsTempDir}/data"
      doClose(new RowProducer[Array[Byte]](testUtil.producerConfig.toProperties, new ByteArraySerializer)) { producer =>
        {
          0 until rowCount foreach (_ =>
            producer.send(new ProducerRecord[Array[Byte], Row](topicName, ByteUtil.toBytes("key"), row)))
          producer.flush()
        }
      }
      val localURL = s"file://${testUtil.hdfsTempDir()}"
      testUtil
        .sinkConnectorCreator()
        .name("my_sink_connector")
        .connectorClass(classOf[HDFSSinkConnector])
        .topic(topicName)
        .taskNumber(sinkTasks)
        .disableConverter
        .config(Map(
          flushLineCountName -> flushLineCount,
          tmpDirName -> tmpDirPath,
          hdfsURLName -> localURL,
          hdfsCreatorClassName -> hdfsCreatorClassNameValue,
          dataDirName -> dataDirPath
        ))
        .run()

      Thread.sleep(20000);
      val partitionID: String = "partition0"
      testUtil.await(() => storage.list(s"$dataDirPath/$topicName/$partitionID").size == 10, 10 seconds)

      testUtil.await(
        () =>
          FileUtils.getStopOffset(
            storage.list(s"$dataDirPath/$topicName/$partitionID").map(x => new Path(x).getName()).toList) == 99,
        10 seconds)

      testUtil.await(() =>
                       storage
                         .list(s"$dataDirPath/$topicName/$partitionID")
                         .map(x => new Path(x).getName())
                         .contains("part-000000090-000000099.csv"),
                     10 seconds)
    }
  }
}

class SimpleHDFSSinkConnector extends HDFSSinkConnector {
  override protected def _taskClass(): Class[_ <: RowSinkTask] = {
    classOf[SimpleHDFSSinkTask]
  }
}

class SimpleHDFSSinkTask extends HDFSSinkTask {
  override def start(props: util.Map[String, String]): Unit = {
    super.start(props)
    props.forEach((key, value) => {
      SimpleHDFSSinkTask.taskProps.put(key, value)
    })
    SimpleHDFSSinkTask.sinkConnectorConfig = hdfsSinkConnectorConfig
  }
}

object SimpleHDFSSinkTask {
  val taskProps = new ConcurrentHashMap[String, String]
  var sinkConnectorConfig: HDFSSinkConnectorConfig = _

}
