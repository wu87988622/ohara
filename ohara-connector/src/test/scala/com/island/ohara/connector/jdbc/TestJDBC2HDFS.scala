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

package com.island.ohara.connector.jdbc

import java.io.{BufferedReader, InputStreamReader}
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.{Calendar, TimeZone}

import com.island.ohara.client.configurator.v0.QueryApi.RdbColumn
import com.island.ohara.client.database.DatabaseClient
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.connector.hdfs.creator.StorageCreator
import com.island.ohara.connector.hdfs.storage.{HDFSStorage, Storage}
import com.island.ohara.connector.hdfs.{HDFSSinkConnector, HDFSSinkConnectorConfig, _}
import com.island.ohara.connector.jdbc.source._
import com.island.ohara.kafka.connector.TaskSetting
import com.island.ohara.kafka.connector.json.{ConnectorKey, TopicKey}
import com.island.ohara.testing.With3Brokers3Workers
import com.island.ohara.testing.service.Hdfs
import org.apache.hadoop.fs.{FileSystem, Path}
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._
class TestJDBC2HDFS extends With3Brokers3Workers with Matchers {
  private[this] val db = testUtil().dataBase()
  private[this] val client = DatabaseClient.builder.url(db.url()).user(db.user()).password(db.password()).build
  private[this] val tableName = CommonUtils.randomString(10)
  private[this] val timestampColumnName = "CREATE_DATE"
  private[this] val workerClient = WorkerClient(testUtil.workersConnProps)

  private[this] def jdbcConfig(settings: Map[String, String]): JDBCSourceConnectorConfig =
    JDBCSourceConnectorConfig(TaskSetting.of(settings.asJava))

  private[this] def hdfsConfig(settings: Map[String, String]): HDFSSinkConnectorConfig =
    HDFSSinkConnectorConfig(TaskSetting.of(settings.asJava))

  private[this] val jdbcProps = jdbcConfig(
    Map(DB_URL -> db.url,
        DB_USERNAME -> db.user,
        DB_PASSWORD -> db.password,
        DB_TABLENAME -> tableName,
        TIMESTAMP_COLUMN_NAME -> timestampColumnName))

  private[this] val hdfsProps = hdfsConfig(
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

    val cal = Calendar.getInstance(TimeZone.getTimeZone(System.getProperty("user.timezone")))
    cal.setTime(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2018-09-01 00:00:00"))
    def subtract(t: Duration): Timestamp = new Timestamp(cal.getTimeInMillis - t.toMillis)
    import scala.concurrent.duration._
    val state =
      db.connection.prepareStatement(
        s"""INSERT INTO \"$tableName\" (\"CREATE_DATE\", \"ID\", \"NAME\", \"ADDRESS\") VALUES(?, ?, ?, ?)""")
    try {
      (1 to 100).foreach(index => {
        state.setTimestamp(1,
                           subtract(index days),
                           Calendar.getInstance(TimeZone.getTimeZone(System.getProperty("user.timezone"))))
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
    val jdbcSourceConnectorKey: ConnectorKey =
      ConnectorKey.of(CommonUtils.randomString(5), "jdbc-source-connector-it-test")
    val hdfsSinkConnectorKey: ConnectorKey = ConnectorKey.of(CommonUtils.randomString(5), "hdfs-sink-connector-it-test")
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))

    Await.result(
      workerClient
        .connectorCreator()
        .connectorKey(jdbcSourceConnectorKey)
        .connectorClass(classOf[JDBCSourceConnector])
        .topicKey(topicKey)
        .numberOfTasks(1)
        .settings(jdbcProps.toMap)
        .create(),
      10 seconds
    )

    Await.result(
      workerClient
        .connectorCreator()
        .connectorKey(hdfsSinkConnectorKey)
        .connectorClass(classOf[HDFSSinkConnector])
        .topicKey(topicKey)
        .settings(hdfsProps.toMap)
        .numberOfTasks(1)
        .create(),
      10 seconds
    )

    try {
      val storage = new HDFSStorage(testUtil.hdfs.fileSystem)
      val hdfsResultFolder = s"${testUtil.hdfs.tmpDirectory}/data/${topicKey.topicNameOnKafka}/partition0"

      CommonUtils.await(() => storage.list(hdfsResultFolder).size == 2, java.time.Duration.ofSeconds(20))

      val fileSystem: FileSystem = testUtil.hdfs.fileSystem
      val resultPath1: String = s"$hdfsResultFolder/part-000000050-000000100.csv"
      val lineCountFile1 = {
        val reader = new BufferedReader(new InputStreamReader(fileSystem.open(new Path(resultPath1))))
        try Iterator.continually(reader.readLine()).takeWhile(_ != null).toArray
        finally reader.close()
      }
      lineCountFile1.length shouldBe 51

      val resultPath2: String = s"$hdfsResultFolder/part-000000000-000000050.csv"
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
      workerClient.delete(jdbcSourceConnectorKey)
      workerClient.delete(hdfsSinkConnectorKey)
    }
  }

  @After
  def afterTest(): Unit = Releasable.close(client)
}

class LocalHDFSStorageCreator(config: HDFSSinkConnectorConfig) extends StorageCreator {
  // TODO: we SHOULD NOT import hdfs directly... by chia
  private[this] val hdfs = Hdfs.local()

  override def create(): Storage = new HDFSStorage(hdfs.fileSystem())

  override def close(): Unit = Releasable.close(hdfs)
}
