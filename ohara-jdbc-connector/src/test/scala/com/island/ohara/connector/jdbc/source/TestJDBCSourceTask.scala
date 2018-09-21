package com.island.ohara.connector.jdbc.source

import java.sql.{Statement, Timestamp}
import com.island.ohara.client.ConfiguratorJson.{Column, RdbColumn}
import com.island.ohara.client.DatabaseClient
import com.island.ohara.connector.jdbc.util.ColumnInfo
import com.island.ohara.data.Row
import com.island.ohara.integration.LocalDataBase
import com.island.ohara.kafka.connector.{RowSourceRecord, TaskConfig}
import com.island.ohara.rule.MediumTest
import com.island.ohara.serialization.DataType
import org.junit.{Before, Test}
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._

class TestJDBCSourceTask extends MediumTest with Matchers with MockitoSugar {
  private[this] val db = LocalDataBase.mysql()
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
    val taskConfig: TaskConfig = mock[TaskConfig]
    val maps: Map[String, String] = Map(DB_URL -> db.url,
                                        DB_USERNAME -> db.user,
                                        DB_PASSWORD -> db.password,
                                        DB_TABLENAME -> tableName,
                                        DB_SCHEMA_PATTERN -> "",
                                        TIMESTAMP_COLUMN_NAME -> timestampColumnName)
    when(taskConfig.options).thenReturn(maps)

    val columns: Seq[Column] = Seq(Column("COLUMN1", DataType.OBJECT, 0),
                                   Column("COLUMN2", DataType.STRING, 1),
                                   Column("COLUMN4", DataType.INT, 3))

    when(taskConfig.schema).thenReturn(columns)
    when(taskConfig.topics).thenReturn(Seq("topic1"))

    jdbcSourceTask._start(taskConfig)

    val rows: Seq[RowSourceRecord] = jdbcSourceTask._poll()
    rows(0).row.cell(0).value.toString() shouldBe "2018-09-01 00:00:00.0"
    rows(0).row.cell(1).value shouldBe "a11"
    rows(0).row.cell(2).value shouldBe 1
  }

  @Test
  def testRowTimestamp(): Unit = {
    val jdbcSourceTask: JDBCSourceTask = new JDBCSourceTask()
    val schema: Seq[Column] = Seq(Column("COLUMN1", DataType.OBJECT, 0))
    val columnInfo: Seq[ColumnInfo] = Seq(ColumnInfo("COLUMN1", "timestamp", new Timestamp(0)))
    val row0: Row = jdbcSourceTask.row(schema, columnInfo)
    row0.cell("COLUMN1").value.toString() shouldBe "1970-01-01 08:00:00.0"
  }

  @Test
  def testRowInt(): Unit = {
    val jdbcSourceTask: JDBCSourceTask = new JDBCSourceTask()
    val schema: Seq[Column] = Seq(Column("COLUMN1", DataType.INT, 0))
    val columnInfo: Seq[ColumnInfo] = Seq(ColumnInfo("COLUMN1", "int", new Integer(100)))
    val row0: Row = jdbcSourceTask.row(schema, columnInfo)
    row0.cell("COLUMN1").value shouldBe 100
  }

  @Test
  def testCellOrder(): Unit = {
    val jdbcSourceTask: JDBCSourceTask = new JDBCSourceTask()
    val schema: Seq[Column] = Seq(Column("c1", DataType.INT, 1), Column("c0", DataType.INT, 0))
    val columnInfo: Seq[ColumnInfo] =
      Seq(ColumnInfo("c1", "int", new Integer(100)), ColumnInfo("c0", "int", new Integer(50)))
    val cells = jdbcSourceTask.row(schema, columnInfo).toSeq
    cells(0).name shouldBe "c0"
    cells(0).value shouldBe 50
    cells(1).name shouldBe "c1"
    cells(1).value shouldBe 100
  }
}
