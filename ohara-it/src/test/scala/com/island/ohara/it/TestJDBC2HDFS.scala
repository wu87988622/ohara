package com.island.ohara.it
import java.io.{BufferedReader, InputStreamReader}
import java.sql.Timestamp
import java.text.SimpleDateFormat

import com.island.ohara.client.ConfiguratorJson.RdbColumn
import com.island.ohara.client.{ConnectorClient, DatabaseClient}
import com.island.ohara.common.util.{ReleaseOnce, CommonUtil}
import com.island.ohara.connector.hdfs.creator.StorageCreator
import com.island.ohara.connector.hdfs.storage.{HDFSStorage, Storage}
import com.island.ohara.connector.hdfs.{HDFSSinkConnector, HDFSSinkConnectorConfig, _}
import com.island.ohara.connector.jdbc.JDBCSourceConnector
import com.island.ohara.connector.jdbc.source._
import com.island.ohara.integration.{Database, OharaTestUtil, With3Brokers3Workers}
import org.apache.hadoop.fs.{FileSystem, Path}
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestJDBC2HDFS extends With3Brokers3Workers with Matchers {
  private[this] val db = Database.of()
  private[this] val client = DatabaseClient(db.url, db.user, db.password)
  private[this] val tableName = "testtable"
  private[this] val timestampColumnName = "CREATE_DATE"
  private[this] val connectorClient = ConnectorClient(testUtil.workersConnProps)

  private[this] val jdbcProps = JDBCSourceConnectorConfig(
    Map(DB_URL -> db.url,
        DB_USERNAME -> db.user,
        DB_PASSWORD -> db.password,
        DB_TABLENAME -> tableName,
        TIMESTAMP_COLUMN_NAME -> timestampColumnName,
        DB_SCHEMA_PATTERN -> ""))

  private[this] val hdfsProps = HDFSSinkConnectorConfig(
    Map(
      FLUSH_LINE_COUNT -> "50",
      TMP_DIR -> s"${testUtil.hdfs.tmpDirectory}/tmp",
      HDFS_URL -> s"file://${testUtil.hdfs.tmpDirectory}",
      HDFS_STORAGE_CREATOR_CLASS -> classOf[LocalHDFSStorageCreator].getName,
      DATAFILE_NEEDHEADER -> "true",
      DATA_DIR -> s"${testUtil.hdfs.tmpDirectory}/data"
    )
  )

  @Before
  def setup(): Unit = {
    val id = RdbColumn("ID", "INTEGER", false)
    val name = RdbColumn("NAME", "VARCHAR(45)", false)
    val address = RdbColumn("ADDRESS", "VARCHAR(45)", false)
    val createDateTimestamp = RdbColumn("CREATE_DATE", "TIMESTAMP", true)
    client.createTable(tableName, Seq(createDateTimestamp, id, name, address))

    val initTime = new Timestamp(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2018-09-01 00:00:00").getTime)
    def subtract(t: Duration): Timestamp = new Timestamp(initTime.getTime - t.toMillis)
    import scala.concurrent.duration._
    val state =
      db.connection.prepareStatement(
        s"""INSERT INTO \"$tableName\" (\"CREATE_DATE\", \"ID\", \"NAME\", \"ADDRESS\") VALUES(?, ?, ?, ?)""")
    try {
      (1 to 100).foreach(index => {
        state.setTimestamp(1, subtract(index days))
        state.setInt(2, index)
        state.setString(3, s"NAME-$index")
        state.setString(4, s"ADDRESS-$index")
        state.addBatch()
      })
      state.executeBatch()
      if (!db.connection.getAutoCommit) db.connection.commit()
    } finally state.close()
  }

  @Test
  def testNormalCase(): Unit = {
    val jdbcSourceConnectorName: String = "jdbc-source-connector-it-test"
    val hdfsSinkConnectorName: String = "hdfs-sink-connector-it-test"
    val topicName: String = "it-test"

    connectorClient
      .connectorCreator()
      .name(jdbcSourceConnectorName)
      .connectorClass(classOf[JDBCSourceConnector])
      .topic(topicName)
      .numberOfTasks(1)
      .configs(jdbcProps.toMap)
      .disableConverter()
      .create()

    connectorClient
      .connectorCreator()
      .name(hdfsSinkConnectorName)
      .connectorClass(classOf[HDFSSinkConnector])
      .topic(topicName)
      .configs(hdfsProps.toMap)
      .numberOfTasks(1)
      .disableConverter()
      .create()

    try {
      val storage = new HDFSStorage(testUtil.hdfs.fileSystem)
      val hdfsResultFolder = s"${testUtil.hdfs.tmpDirectory}/data/$topicName/partition0"
      CommonUtil.await(() => storage.list(hdfsResultFolder).size == 2, java.time.Duration.ofSeconds(20))

      val fileSystem: FileSystem = testUtil.hdfs.fileSystem
      val resultPath1: String = s"$hdfsResultFolder/part-000000050-000000099.csv"
      val lineCountFile1 = {
        val reader = new BufferedReader(new InputStreamReader(fileSystem.open(new Path(resultPath1))))
        try Iterator.continually(reader.readLine()).takeWhile(_ != null).toArray
        finally reader.close()
      }
      lineCountFile1.length shouldBe 51

      val resultPath2: String = s"$hdfsResultFolder/part-000000000-000000049.csv"
      val lineCountFile2 = {
        val reader = new BufferedReader(new InputStreamReader(fileSystem.open(new Path(resultPath2))))
        try Iterator.continually(reader.readLine()).takeWhile(_ != null).toArray
        finally reader.close()
      }
      lineCountFile2.length shouldBe 51
      val header: String = lineCountFile1(0)
      header shouldBe "CREATE_DATE,ID,NAME,ADDRESS"
      lineCountFile1(1) shouldBe "2018-07-13 00:00:00.0,50,NAME-50,ADDRESS-50"
      lineCountFile1(50) shouldBe "2018-08-31 00:00:00.0,1,NAME-1,ADDRESS-1"
    } finally {
      connectorClient.delete(jdbcSourceConnectorName)
      connectorClient.delete(hdfsSinkConnectorName)
    }
  }

  @After
  def afterTest(): Unit = {
    client.dropTable(tableName)
    ReleaseOnce.close(connectorClient)
    ReleaseOnce.close(client)
  }
}

class LocalHDFSStorageCreator(config: HDFSSinkConnectorConfig) extends StorageCreator {
  private[this] val fileSystem: FileSystem = OharaTestUtil.localHDFS().hdfs.fileSystem
  private[this] val hdfsStorage: HDFSStorage = new HDFSStorage(fileSystem)

  override def storage(): Storage = {
    hdfsStorage
  }

  override def close(): Unit = {
    ReleaseOnce.close(fileSystem)
  }
}
