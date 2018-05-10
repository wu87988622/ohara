package com.island.ohara.hdfs

import java.util
import java.util.concurrent.ConcurrentHashMap

import com.island.ohara.integration._
import com.island.ohara.rule.MediumTest
import com.island.ohara.io.CloseOnce._
import com.island.ohara.kafka.connector.RowSinkTask
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
    val hdfsURL = "hdfs://test:9000"

    doClose(new OharaTestUtil(3, 1)) { testUtil =>
      val resp: (Int, String) =
        testUtil.startConnector(s"""{"name":"my_sink_connector", "config":{"connector.class":"${classOf[
          SimpleHDFSSinkConnector].getName}","topics":"my_connector_topic", "tasks.max":"$sinkTasks",
          "$flushLineCountName": "$flushLineCount", "$tmpDirName": "$tmpDirPath", "$hdfsURLName": "$hdfsURL"}}""")

      testUtil.await(() => SimpleHDFSSinkTask.taskProps.get("topics") == "my_connector_topic", 20 second)
      testUtil.await(() => SimpleHDFSSinkTask.taskProps.get(flushLineCountName) == flushLineCount, 20 second)
      testUtil.await(() => SimpleHDFSSinkTask.taskProps.get(rotateIntervalMSName) == null, 20 second)
      testUtil.await(() => SimpleHDFSSinkTask.taskProps.get(tmpDirName) == tmpDirPath, 10 second)
      testUtil.await(() => SimpleHDFSSinkTask.sinkConnectorConfig.dataDir() == "/data", 20 second)
      testUtil.await(() => SimpleHDFSSinkTask.sinkConnectorConfig.flushLineCount() == 2000, 20 second)
      testUtil.await(() => SimpleHDFSSinkTask.sinkConnectorConfig.offsetInconsistentSkip() == false, 20 second)
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
