package com.island.ohara.connector.hdfs

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.time.Duration
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.client.ConnectorClient
import com.island.ohara.common.data.{Cell, DataType, Row, Serializer}
import com.island.ohara.common.util.{ByteUtil, CloseOnce, CommonUtil}
import com.island.ohara.connector.hdfs.creator.LocalHDFSStorageCreator
import com.island.ohara.connector.hdfs.storage.HDFSStorage
import com.island.ohara.integration._
import com.island.ohara.kafka.Producer
import com.island.ohara.kafka.connector.{RowSinkTask, TaskConfig}
import org.apache.hadoop.fs.Path
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._

class TestHDFSSinkConnector extends With3Brokers3Workers with Matchers {
  private[this] val connectorClient = ConnectorClient(testUtil.workersConnProps)
  private[this] val hdfsURL: String = "hdfs://host1:9000"
  private[this] val tmpDir: String = "/tmp"

  private[this] val schema = Seq(Column("cf0", DataType.BOOLEAN, 1))
  @Test
  def testTaskConfigs(): Unit = {
    val maxTasks = 5
    val hdfsSinkConnector = new HDFSSinkConnector()

    hdfsSinkConnector._start(
      new TaskConfig("test", Seq("topic").asJava, Seq.empty.asJava, Map(HDFS_URL -> hdfsURL, TMP_DIR -> tmpDir).asJava))
    val result = hdfsSinkConnector._taskConfigs(maxTasks)

    result.size shouldBe maxTasks
    result.asScala.foreach(r => {
      r.options.get(HDFS_URL) shouldBe hdfsURL
      r.options.get(TMP_DIR) shouldBe tmpDir
    })
  }

  @Test
  def testRunMiniClusterAndAssignConfig(): Unit = {
    val connectorName = methodName
    val topicName = methodName
    val sinkTasks = 2
    val flushLineCountName = FLUSH_LINE_COUNT
    val flushLineCount = "2000"
    val rotateIntervalMSName = ROTATE_INTERVAL_MS
    val tmpDirName = TMP_DIR
    val tmpDirPath = "/home/tmp"
    val hdfsURLName = HDFS_URL

    val localURL = s"file://${testUtil.hdfs.tmpDirectory}"
    connectorClient
      .connectorCreator()
      .name(connectorName)
      .connectorClass(classOf[SimpleHDFSSinkConnector])
      .topic(topicName)
      .numberOfTasks(sinkTasks)
      .disableConverter()
      .configs(Map(flushLineCountName -> flushLineCount, tmpDirName -> tmpDirPath, hdfsURLName -> localURL))
      .schema(schema)
      .create()

    CommonUtil
      .await(() => SimpleHDFSSinkTask.taskProps.get(flushLineCountName) == flushLineCount, Duration.ofSeconds(20))
    CommonUtil.await(() => SimpleHDFSSinkTask.taskProps.get(rotateIntervalMSName) == null, Duration.ofSeconds(20))
    CommonUtil.await(() => SimpleHDFSSinkTask.taskProps.get(tmpDirName) == tmpDirPath, Duration.ofSeconds(10))
    CommonUtil.await(() => SimpleHDFSSinkTask.sinkConnectorConfig.dataDir == "/data", Duration.ofSeconds(20))
    CommonUtil.await(() => SimpleHDFSSinkTask.sinkConnectorConfig.flushLineCount == 2000, Duration.ofSeconds(20))
  }

  @Test
  def testHDFSSinkConnector(): Unit = {
    val sinkTasks = 1
    val flushLineCountName = FLUSH_LINE_COUNT
    val flushLineCount = "10"
    val tmpDirName = TMP_DIR
    val dataDirName = DATA_DIR
    val isHeader = DATAFILE_NEEDHEADER
    val hdfsURLName = HDFS_URL
    val connectorName = methodName
    val topicName = methodName
    val rowCount = 100
    val row = Row.of(Cell.of("cf0", 10), Cell.of("cf1", 11))
    val hdfsCreatorClassName = HDFS_STORAGE_CREATOR_CLASS
    val hdfsCreatorClassNameValue = classOf[LocalHDFSStorageCreator].getName

    val fileSystem = testUtil.hdfs.fileSystem()
    val storage = new HDFSStorage(fileSystem)
    val tmpDirPath = s"${testUtil.hdfs.tmpDirectory}/tmp"
    val dataDirPath = s"${testUtil.hdfs.tmpDirectory}/data"
    val producer = Producer.builder().brokers(testUtil.brokersConnProps).build(Serializer.BYTES, Serializer.ROW)
    try {
      0 until rowCount foreach (_ => producer.sender().key(ByteUtil.toBytes("key")).value(row).send(topicName))
      producer.flush()
    } finally producer.close()

    val localURL = s"file://${testUtil.hdfs.tmpDirectory}"
    connectorClient
      .connectorCreator()
      .name(connectorName)
      .connectorClass(classOf[HDFSSinkConnector])
      .topic(topicName)
      .numberOfTasks(sinkTasks)
      .disableConverter()
      .configs(Map(
        flushLineCountName -> flushLineCount,
        tmpDirName -> tmpDirPath,
        hdfsURLName -> localURL,
        hdfsCreatorClassName -> hdfsCreatorClassNameValue,
        dataDirName -> dataDirPath,
        isHeader -> "false"
      ))
      .schema(schema)
      .create()

    TimeUnit.SECONDS.sleep(5)
    val partitionID: String = "partition0"
    CommonUtil.await(() => storage.list(s"$dataDirPath/$topicName/$partitionID").size == 10, Duration.ofSeconds(20))

    CommonUtil.await(
      () =>
        FileUtils.getStopOffset(storage.list(s"$dataDirPath/$topicName/$partitionID").map(FileUtils.fileName(_))) == 99,
      Duration.ofSeconds(20))

    CommonUtil.await(() =>
                       storage
                         .list(s"$dataDirPath/$topicName/$partitionID")
                         .map(FileUtils.fileName)
                         .contains("part-000000090-000000099.csv"),
                     Duration.ofSeconds(20))

    val path: Path = new Path(s"$dataDirPath/$topicName/$partitionID/part-000000090-000000099.csv")
    val file: InputStream = testUtil.hdfs.fileSystem.open(path)
    val streamReader: InputStreamReader = new InputStreamReader(file)
    val bufferedReaderStream: BufferedReader = new BufferedReader(streamReader)
    try {
      val rowDataCount = Stream.continually(bufferedReaderStream.readLine()).takeWhile(_ != null).toList.size
      rowDataCount shouldBe 10
    } finally {
      bufferedReaderStream.close()
      streamReader.close()
      file.close()
    }
  }

  @Test
  def testRecoverOffset(): Unit = {
    val sinkTasks = 1
    val flushLineCountName = FLUSH_LINE_COUNT
    val flushLineCount = "100"
    val tmpDirName = TMP_DIR
    val dataDirName = DATA_DIR
    val hdfsURLName = HDFS_URL
    val needHeader = DATAFILE_NEEDHEADER
    val connectorName = methodName
    val topicName = methodName
    val rowCount = 200
    val row = Row.of(Cell.of("cf0", 10), Cell.of("cf1", 11))
    val hdfsCreatorClassName = HDFS_STORAGE_CREATOR_CLASS
    val hdfsCreatorClassNameValue = classOf[LocalHDFSStorageCreator].getName

    val fileSystem = testUtil.hdfs.fileSystem()
    val storage = new HDFSStorage(fileSystem)
    val tmpDirPath = s"${testUtil.hdfs.tmpDirectory}/tmp"
    val dataDirPath = s"${testUtil.hdfs.tmpDirectory}/data"

    //Before running the Kafka Connector, create the file to local hdfs for test recover offset
    val partitionID: String = "partition0"
    fileSystem.createNewFile(new Path(s"$dataDirPath/$topicName/$partitionID/part-000000000-000000099.csv"))

    val producer = Producer.builder().brokers(testUtil.brokersConnProps).build(Serializer.BYTES, Serializer.ROW)
    try {
      0 until rowCount foreach (_ => producer.sender().key(ByteUtil.toBytes("key")).value(row).send(topicName))
      producer.flush()
    } finally producer.close()

    val localURL = s"file://${testUtil.hdfs.tmpDirectory}"
    connectorClient
      .connectorCreator()
      .name(connectorName)
      .connectorClass(classOf[HDFSSinkConnector])
      .topic(topicName)
      .numberOfTasks(sinkTasks)
      .disableConverter()
      .configs(Map(
        flushLineCountName -> flushLineCount,
        tmpDirName -> tmpDirPath,
        hdfsURLName -> localURL,
        hdfsCreatorClassName -> hdfsCreatorClassNameValue,
        needHeader -> "true",
        dataDirName -> dataDirPath
      ))
      .schema(schema)
      .create()

    TimeUnit.SECONDS.sleep(5)
    storage
      .list(s"$dataDirPath/$topicName/$partitionID")
      .foreach(x => {
        println(s"file path: $x")
      })
    CommonUtil.await(() => storage.list(s"$dataDirPath/$topicName/$partitionID").size == 2, Duration.ofSeconds(20))

    CommonUtil.await(() =>
                       FileUtils.getStopOffset(
                         storage.list(s"$dataDirPath/$topicName/$partitionID").map(FileUtils.fileName(_))) == 199,
                     Duration.ofSeconds(20))

    CommonUtil.await(() =>
                       storage
                         .list(s"$dataDirPath/$topicName/$partitionID")
                         .map(FileUtils.fileName)
                         .contains("part-000000100-000000199.csv"),
                     Duration.ofSeconds(20))

    val path: Path = new Path(s"$dataDirPath/$topicName/$partitionID/part-000000100-000000199.csv")
    val file: InputStream = testUtil.hdfs.fileSystem.open(path)
    val streamReader: InputStreamReader = new InputStreamReader(file)
    val bufferedReaderStream: BufferedReader = new BufferedReader(streamReader)
    try {
      val rowDataCount = Stream.continually(bufferedReaderStream.readLine()).takeWhile(_ != null).toList.size
      rowDataCount shouldBe 101
    } finally {
      bufferedReaderStream.close()
      streamReader.close()
      file.close()
    }
  }

  @Test
  def testDataNotMappingSchema(): Unit = {

    val sinkTasks = 1
    val flushLineCountName = FLUSH_LINE_COUNT
    val flushLineCount = "10"
    val tmpDirName = TMP_DIR
    val dataDirName = DATA_DIR
    val isHeader = DATAFILE_NEEDHEADER
    val hdfsURLName = HDFS_URL
    val connectorName = methodName
    val topicName = methodName
    val rowCount = 100
    val row = Row.of(Cell.of("cf0", 10), Cell.of("cf1", 11))
    val hdfsCreatorClassName = HDFS_STORAGE_CREATOR_CLASS
    val hdfsCreatorClassNameValue = classOf[LocalHDFSStorageCreator].getName

    val fileSystem = testUtil.hdfs.fileSystem
    val storage = new HDFSStorage(fileSystem)
    val tmpDirPath = s"${testUtil.hdfs.tmpDirectory}/tmp"
    val dataDirPath = s"${testUtil.hdfs.tmpDirectory}/data"

    val producer = Producer.builder().brokers(testUtil.brokersConnProps).build(Serializer.BYTES, Serializer.ROW)
    try {
      0 until rowCount foreach (_ => producer.sender().key(ByteUtil.toBytes("key")).value(row).send(topicName))
      producer.flush()
    } finally producer.close()

    val localURL = s"file://${testUtil.hdfs.tmpDirectory}"
    connectorClient
      .connectorCreator()
      .name(connectorName)
      .connectorClass(classOf[HDFSSinkConnector])
      .topic(topicName)
      .numberOfTasks(sinkTasks)
      .disableConverter()
      .configs(Map(
        flushLineCountName -> flushLineCount,
        tmpDirName -> tmpDirPath,
        hdfsURLName -> localURL,
        hdfsCreatorClassName -> hdfsCreatorClassNameValue,
        dataDirName -> dataDirPath,
        isHeader -> "false"
      ))
      .schema(Seq(Column("cccc", DataType.BOOLEAN, 1)))
      .create()

    TimeUnit.SECONDS.sleep(5)
    val partitionID: String = "partition0"
    CommonUtil.await(() => storage.list(s"$dataDirPath/$topicName/$partitionID").size == 0, Duration.ofSeconds(20))
  }

  @After
  def tearDown(): Unit = CloseOnce.close(connectorClient)
}

class SimpleHDFSSinkConnector extends HDFSSinkConnector {
  override protected def _taskClass(): Class[_ <: RowSinkTask] = {
    classOf[SimpleHDFSSinkTask]
  }
}

class SimpleHDFSSinkTask extends HDFSSinkTask {
  override def _start(props: TaskConfig): Unit = {
    super._start(props)
    props.options.asScala.foreach {
      case (k, v) => SimpleHDFSSinkTask.taskProps.put(k, v)
    }
    SimpleHDFSSinkTask.sinkConnectorConfig = hdfsSinkConnectorConfig
  }
}

object SimpleHDFSSinkTask {
  val taskProps = new ConcurrentHashMap[String, String]
  var sinkConnectorConfig: HDFSSinkConnectorConfig = _
}
