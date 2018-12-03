package com.island.ohara.connector.jdbc.source

import java.sql.{Statement, Timestamp}

import com.island.ohara.client.ConfiguratorJson.{Column, RdbColumn}
import com.island.ohara.client.DatabaseClient
import com.island.ohara.common.data.{DataType, Row}
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.connector.jdbc.util.ColumnInfo
import com.island.ohara.integration.Database
import com.island.ohara.kafka.connector.{RowSourceRecord, TaskConfig}
import org.apache.kafka.connect.source.SourceTaskContext
import org.apache.kafka.connect.storage.OffsetStorageReader
import org.junit.{Before, Test}
import org.mockito.Mockito._
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar

import scala.collection.JavaConverters._

class TestJDBCSourceTask extends MediumTest with Matchers with MockitoSugar {
  private[this] val db = Database.of()
  private[this] val client = DatabaseClient(db.url, db.user, db.password)
  private[this] val tableName = "TABLE1"
  private[this] val timestampColumnName = "COLUMN1"

  @Before
  def setup(): Unit = {
    val column1 = RdbColumn("COLUMN1", "TIMESTAMP", true)
    val column2 = RdbColumn("COLUMN2", "varchar(45)", false)
    val column3 = RdbColumn("COLUMN3", "VARCHAR(45)", false)
    val column4 = RdbColumn("COLUMN4", "integer", false)

    client.createTable(tableName, Seq(column1, column2, column3, column4))
    val statement: Statement = db.connection.createStatement()

    statement.executeUpdate(
      s"INSERT INTO $tableName(COLUMN1,COLUMN2,COLUMN3,COLUMN4) VALUES('2018-09-01 00:00:00', 'a11', 'a12', 1)")
    statement.executeUpdate(
      s"INSERT INTO $tableName(COLUMN1,COLUMN2,COLUMN3,COLUMN4) VALUES('2018-09-01 00:00:01', 'a21', 'a22', 2)")
    statement.executeUpdate(
      s"INSERT INTO $tableName(COLUMN1,COLUMN2,COLUMN3,COLUMN4) VALUES('2018-09-01 00:00:02', 'a31', 'a32', 3)")
    statement.executeUpdate(
      s"INSERT INTO $tableName(COLUMN1,COLUMN2,COLUMN3,COLUMN4) VALUES(NOW() + INTERVAL 3 MINUTE, 'a41', 'a42', 4)")
    statement.executeUpdate(
      s"INSERT INTO $tableName(column1,column2,column3,column4) VALUES(NOW() + INTERVAL 1 DAY, 'a51', 'a52', 5)")
  }

  @Test
  def testPoll(): Unit = {
    val jdbcSourceTask: JDBCSourceTask = new JDBCSourceTask()
    val taskContext: SourceTaskContext = mock[SourceTaskContext]
    val offsetStorageReader: OffsetStorageReader = mock[OffsetStorageReader]
    when(taskContext.offsetStorageReader()).thenReturn(offsetStorageReader)
    jdbcSourceTask.initialize(taskContext.asInstanceOf[SourceTaskContext])

    val taskConfig: TaskConfig = mock[TaskConfig]
    val maps: Map[String, String] = Map(DB_URL -> db.url,
                                        DB_USERNAME -> db.user,
                                        DB_PASSWORD -> db.password,
                                        DB_TABLENAME -> tableName,
                                        DB_SCHEMA_PATTERN -> "",
                                        TIMESTAMP_COLUMN_NAME -> timestampColumnName)
    when(taskConfig.options).thenReturn(maps.asJava)

    val columns: Seq[Column] = Seq(Column("COLUMN1", DataType.OBJECT, 0),
                                   Column("COLUMN2", DataType.STRING, 1),
                                   Column("COLUMN4", DataType.INT, 3))

    when(taskConfig.schema).thenReturn(columns.asJava)
    when(taskConfig.topics).thenReturn(Seq("topic1").asJava)
    jdbcSourceTask._start(taskConfig)

    val rows: Seq[RowSourceRecord] = jdbcSourceTask._poll().asScala
    rows(0).row.cell(0).value.toString() shouldBe "2018-09-01 00:00:00.0"
    rows(0).row.cell(1).value shouldBe "a11"
    rows(0).row.cell(2).value shouldBe 1

    rows(0).row.cell(0).name shouldBe "COLUMN1"
    rows(1).row.cell(1).name shouldBe "COLUMN2"
    rows(2).row.cell(2).name shouldBe "COLUMN4"

    //Test row 1 offset
    rows(0).sourceOffset.asScala.foreach(x => {
      x._1 shouldBe JDBCSourceTask.DB_TABLE_OFFSET_KEY
      x._2 shouldBe 1535731200000L
    })
    //Test row 2 offset
    rows(1).sourceOffset.asScala.foreach(x => {
      x._1 shouldBe JDBCSourceTask.DB_TABLE_OFFSET_KEY
      x._2 shouldBe 1535731201000L
    })
  }

  @Test
  def testRowTimestamp(): Unit = {
    val jdbcSourceTask: JDBCSourceTask = new JDBCSourceTask()
    val schema: Seq[Column] = Seq(Column("COLUMN1", DataType.OBJECT, 0))
    val columnInfo: Seq[ColumnInfo[Timestamp]] = Seq(ColumnInfo("COLUMN1", "timestamp", new Timestamp(0)))
    val row0: Row = jdbcSourceTask.row(schema, columnInfo)
    row0.cell("COLUMN1").value.toString() shouldBe "1970-01-01 08:00:00.0"
  }

  @Test
  def testRowInt(): Unit = {
    val jdbcSourceTask: JDBCSourceTask = new JDBCSourceTask()
    val schema: Seq[Column] = Seq(Column("COLUMN1", DataType.INT, 0))
    val columnInfo: Seq[ColumnInfo[Int]] = Seq(ColumnInfo("COLUMN1", "int", new Integer(100)))
    val row0: Row = jdbcSourceTask.row(schema, columnInfo)
    row0.cell("COLUMN1").value shouldBe 100
  }

  @Test
  def testCellOrder(): Unit = {
    val jdbcSourceTask: JDBCSourceTask = new JDBCSourceTask()
    val schema: Seq[Column] = Seq(Column("c1", DataType.INT, 1), Column("c0", DataType.INT, 0))
    val columnInfo: Seq[ColumnInfo[Int]] =
      Seq(ColumnInfo("c1", "int", new Integer(100)), ColumnInfo("c0", "int", new Integer(50)))
    val cells = jdbcSourceTask.row(schema, columnInfo).cells().asScala
    cells(0).name shouldBe "c0"
    cells(0).value shouldBe 50
    cells(1).name shouldBe "c1"
    cells(1).value shouldBe 100
  }

  @Test
  def testRowNewName(): Unit = {
    val jdbcSourceTask: JDBCSourceTask = new JDBCSourceTask()
    val schema: Seq[Column] = Seq(Column("COLUMN1", "COLUMN100", DataType.INT, 0))
    val columnInfo: Seq[ColumnInfo[Int]] = Seq(ColumnInfo("COLUMN1", "int", new Integer(100)))
    val row0: Row = jdbcSourceTask.row(schema, columnInfo)
    row0.cell("COLUMN100").value shouldBe 100
  }

  @Test
  def testPollNewName(): Unit = {
    val jdbcSourceTask: JDBCSourceTask = new JDBCSourceTask()
    val taskContext: SourceTaskContext = mock[SourceTaskContext]
    val offsetStorageReader: OffsetStorageReader = mock[OffsetStorageReader]
    when(taskContext.offsetStorageReader()).thenReturn(offsetStorageReader)
    jdbcSourceTask.initialize(taskContext.asInstanceOf[SourceTaskContext])

    val taskConfig: TaskConfig = mock[TaskConfig]
    val maps: Map[String, String] = Map(DB_URL -> db.url,
                                        DB_USERNAME -> db.user,
                                        DB_PASSWORD -> db.password,
                                        DB_TABLENAME -> tableName,
                                        DB_SCHEMA_PATTERN -> "",
                                        TIMESTAMP_COLUMN_NAME -> timestampColumnName)
    when(taskConfig.options).thenReturn(maps.asJava)

    val columns: Seq[Column] = Seq(Column("COLUMN1", "COLUMN100", DataType.OBJECT, 0),
                                   Column("COLUMN2", "COLUMN200", DataType.STRING, 1),
                                   Column("COLUMN4", "COLUMN400", DataType.INT, 3))

    when(taskConfig.schema).thenReturn(columns.asJava)
    when(taskConfig.topics).thenReturn(Seq("topic1").asJava)

    jdbcSourceTask._start(taskConfig)

    val rows: Seq[RowSourceRecord] = jdbcSourceTask._poll().asScala
    rows(0).row.cell(0).value.toString() shouldBe "2018-09-01 00:00:00.0"
    rows(0).row.cell(1).value shouldBe "a11"
    rows(0).row.cell(2).value shouldBe 1

    rows(0).row.cell(0).name shouldBe "COLUMN100"
    rows(1).row.cell(1).name shouldBe "COLUMN200"
    rows(2).row.cell(2).name shouldBe "COLUMN400"
  }

  @Test
  def testDbTimestampColumnValue(): Unit = {
    val jdbcSourceTask: JDBCSourceTask = new JDBCSourceTask()
    val dbColumnInfo: Seq[ColumnInfo[_]] = Seq(ColumnInfo("column1", "string", "value1"),
                                               ColumnInfo("column2", "timestamp", new Timestamp(1537510900000L)),
                                               ColumnInfo("column3", "string", "value3"))
    val timestamp: Long = jdbcSourceTask.dbTimestampColumnValue(dbColumnInfo, "column2")
    timestamp shouldBe 1537510900000L
  }
}
