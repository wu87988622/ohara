package com.island.ohara.it
import java.io.{BufferedReader, InputStreamReader}
import java.sql.Statement

import com.island.ohara.client.ConfiguratorJson.RdbColumn
import com.island.ohara.client.DatabaseClient
import com.island.ohara.connector.hdfs.{HDFSSinkConnector, HDFSSinkConnectorConfig}
import com.island.ohara.connector.hdfs.creator.StorageCreator
import com.island.ohara.connector.hdfs.storage.{HDFSStorage, Storage}
import com.island.ohara.connector.jdbc.JDBCSourceConnector
import com.island.ohara.connector.jdbc.source._
import com.island.ohara.connector.hdfs._
import com.island.ohara.integration.{LocalDataBase, OharaTestUtil, With3Brokers3Workers}
import com.island.ohara.io.CloseOnce
import org.apache.hadoop.fs.{FileSystem, Path}
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestJDBC2HDFS extends With3Brokers3Workers with Matchers {
  private[this] val db = LocalDataBase.mysql()
  private[this] val client = DatabaseClient(db.url, db.user, db.password)
  private[this] val tableName = "testtable"
  private[this] val timestampColumnName = "CREATE_DATE"
  private[this] val connectorClient = testUtil.connectorClient

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
      TMP_DIR -> s"${testUtil.tmpDirectory}/tmp",
      HDFS_URL -> s"file://${testUtil.tmpDirectory}",
      HDFS_STORAGE_CREATOR_CLASS -> classOf[LocalHDFSStorageCreator].getName,
      DATAFILE_NEEDHEADER -> "true",
      DATA_DIR -> s"${testUtil.tmpDirectory}/data"
    )
  )

  @Before
  def setup(): Unit = {
    val id = RdbColumn("ID", "INTEGER", false)
    val name = RdbColumn("NAME", "VARCHAR(45)", false)
    val address = RdbColumn("ADDRESS", "VARCHAR(45)", false)
    val createDateTimestamp = RdbColumn("CREATE_DATE", "TIMESTAMP", true)
    client.createTable(tableName, Seq(createDateTimestamp, id, name, address))
    val statement: Statement = db.connection.createStatement()

    for (i <- 1 to 100) {
      statement.executeUpdate(
        s"INSERT INTO $tableName(CREATE_DATE, ID, NAME, ADDRESS) VALUES('2018-09-01 00:00:00' - INTERVAL $i DAY, $i, 'NAME-$i', 'ADDRESS-$i')")
    }
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

    val storage = new HDFSStorage(testUtil.fileSystem)
    val hdfsResultFolder = s"${testUtil.tmpDirectory}/data/$topicName/partition0"
    OharaTestUtil.await(() => storage.list(hdfsResultFolder).size == 2, 10 seconds)

    val fileSystem: FileSystem = testUtil.fileSystem
    val resultPath1: String = s"${hdfsResultFolder}/part-000000050-000000099.csv"
    val lineCountFile1 =
      CloseOnce.doClose(new BufferedReader(new InputStreamReader(fileSystem.open(new Path(resultPath1))))) { reader =>
        Iterator.continually(reader.readLine()).takeWhile(_ != null).toArray
      }
    lineCountFile1.length shouldBe 51

    val resultPath2: String = s"${hdfsResultFolder}/part-000000000-000000049.csv"
    val lineCountFile2 =
      CloseOnce.doClose(new BufferedReader(new InputStreamReader(fileSystem.open(new Path(resultPath2))))) { reader =>
        Iterator.continually(reader.readLine()).takeWhile(_ != null).toArray
      }
    lineCountFile2.length shouldBe 51
    val header: String = lineCountFile1(0)
    header shouldBe "CREATE_DATE,ID,NAME,ADDRESS"
    lineCountFile1(1) shouldBe "2018-07-13 00:00:00.0,50,NAME-50,ADDRESS-50"
    lineCountFile1(50) shouldBe "2018-08-31 00:00:00.0,1,NAME-1,ADDRESS-1"
  }

  @After
  def afterTest(): Unit = {
    CloseOnce.close(client)
  }
}

class LocalHDFSStorageCreator(config: HDFSSinkConnectorConfig) extends StorageCreator {
  private[this] val fileSystem: FileSystem = OharaTestUtil.localHDFS(1).fileSystem
  private[this] val hdfsStorage: HDFSStorage = new HDFSStorage(fileSystem)

  override def storage(): Storage = {
    hdfsStorage
  }

  override def close(): Unit = {
    CloseOnce.close(fileSystem)
  }
}
