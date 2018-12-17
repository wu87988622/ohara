package com.island.ohara.connector.jdbc.source

import java.sql.{Statement, Timestamp}

import com.island.ohara.client.ConfiguratorJson.RdbColumn
import com.island.ohara.client.DatabaseClient
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.{ReleaseOnce, CommonUtil}
import com.island.ohara.connector.jdbc.util.{ColumnInfo, DateTimeUtils}
import com.island.ohara.integration.Database
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.collection.mutable.ListBuffer

class TestDBTableDataProvider extends MediumTest with Matchers {

  private[this] val db = Database.of()
  private[this] val client = DatabaseClient(db.url, db.user, db.password)
  private[this] val tableName = "table1"

  @Before
  def setup(): Unit = {
    val column1 = RdbColumn("column1", "TIMESTAMP", true)
    val column2 = RdbColumn("column2", "varchar(45)", false)
    val column3 = RdbColumn("column3", "VARCHAR(45)", false)
    val column4 = RdbColumn("column4", "integer", false)

    client.createTable(tableName, Seq(column1, column2, column3, column4))
    val statement: Statement = db.connection.createStatement()

    statement.executeUpdate(
      s"INSERT INTO $tableName(column1,column2,column3,column4) VALUES('2018-09-01 00:00:00', 'a11', 'a12', 1)")
    statement.executeUpdate(
      s"INSERT INTO $tableName(column1,column2,column3,column4) VALUES('2018-09-01 00:00:01', 'a21', 'a22', 2)")
    statement.executeUpdate(
      s"INSERT INTO $tableName(column1,column2,column3,column4) VALUES('2018-09-01 00:00:02', 'a31', 'a32', 3)")
    statement.executeUpdate(
      s"INSERT INTO $tableName(column1,column2,column3,column4) VALUES(NOW() + INTERVAL 3 MINUTE, 'a41', 'a42', 4)")
    statement.executeUpdate(
      s"INSERT INTO $tableName(column1,column2,column3,column4) VALUES(NOW() + INTERVAL 1 DAY, 'a51', 'a52', 5)")
  }

  @Test
  def testRowListResultSet(): Unit = {
    val dbTableDataProvider = new DBTableDataProvider(db.url, db.user, db.password)
    val results: QueryResultIterator = dbTableDataProvider.executeQuery(tableName, "column1", new Timestamp(0)) //0 is 1970-01-01 00:00:00

    var count = 0
    val resultList: ListBuffer[Seq[ColumnInfo[_]]] = new ListBuffer[Seq[ColumnInfo[_]]]
    while (results.hasNext) {
      val listBuffer: Seq[ColumnInfo[_]] = results.next()
      resultList += listBuffer
      count = count + 1
    }
    count shouldBe 3
    resultList(0)(3).columnName shouldBe "column4"
    resultList(0)(3).columnType shouldBe "INT"
    resultList(0)(3).value shouldBe 1
  }

  @Test
  def testDbCurrentTime(): Unit = {
    val dbTableDataProvider = new DBTableDataProvider(db.url, db.user, db.password)
    val dbCurrentTime = dbTableDataProvider.dbCurrentTime(DateTimeUtils.CALENDAR)
    val dbCurrentTimestamp = dbCurrentTime.getTime()
    val systemCurrentTimestamp = CommonUtil.current()
    ((systemCurrentTimestamp - dbCurrentTimestamp) < 5000) shouldBe true
  }

  @Test
  def testColumnList(): Unit = {
    val dbTableDataProvider = new DBTableDataProvider(db.url, db.user, db.password)
    val columns: Seq[RdbColumn] = dbTableDataProvider.columns(tableName)
    columns(0).name shouldBe "column1"
    columns(1).name shouldBe "column2"
    columns(2).name shouldBe "column3"
    columns(3).name shouldBe "column4"
  }

  @Test
  def testTableISNotExists(): Unit = {
    val dbTableDataProvider = new DBTableDataProvider(db.url, db.user, db.password)
    dbTableDataProvider.isTableExists("table100") shouldBe false
  }

  @Test
  def testColumnHaveTable(): Unit = {
    val dbTableDataProvider = new DBTableDataProvider(db.url, db.user, db.password)
    dbTableDataProvider.isTableExists(tableName) shouldBe true
  }
  @After
  def tearDown(): Unit = {
    ReleaseOnce.close(client)
    ReleaseOnce.close(db)
  }
}
