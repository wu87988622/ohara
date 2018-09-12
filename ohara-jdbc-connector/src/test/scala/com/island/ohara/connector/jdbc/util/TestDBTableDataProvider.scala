package com.island.ohara.connector.jdbc.util

import java.sql.{Statement, Timestamp}

import com.island.ohara.client.ConfiguratorJson.RdbColumn
import com.island.ohara.client.DatabaseClient
import com.island.ohara.connector.jdbc.source.QueryResultIterator
import com.island.ohara.integration.LocalDataBase
import com.island.ohara.io.CloseOnce
import com.island.ohara.rule.MediumTest
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

class TestDBTableDataProvider extends MediumTest with Matchers {

  private[this] val db = LocalDataBase.mysql()
  private[this] val client = DatabaseClient(db.url, db.user, db.password)
  private[this] val tableName = "table1"

  @Before
  def setup(): Unit = {
    val column1 = RdbColumn("column1", "TIMESTAMP", true)
    val column2 = RdbColumn("column2", "VARCHAR(45)", false)
    val column3 = RdbColumn("column3", "VARCHAR(45)", false)
    client.createTable(tableName, Seq(column1, column2, column3))
    val statement: Statement = db.connection.createStatement()

    statement.executeUpdate(
      s"INSERT INTO $tableName(column1,column2,column3) VALUES('2018-09-01 00:00:00', 'a11', 'a12')")
    statement.executeUpdate(
      s"INSERT INTO $tableName(column1,column2,column3) VALUES('2018-09-01 00:00:01', 'a21', 'a22')")
    statement.executeUpdate(
      s"INSERT INTO $tableName(column1,column2,column3) VALUES('2018-09-01 00:00:02', 'a31', 'a32')")
    statement.executeUpdate(
      s"INSERT INTO $tableName(column1,column2,column3) VALUES(NOW() + INTERVAL 3 MINUTE, 'a41', 'a42')")
    statement.executeUpdate(
      s"INSERT INTO $tableName(column1,column2,column3) VALUES(NOW() + INTERVAL 1 DAY, 'a51', 'a52')")
  }

  @Test
  def testRowListResultSet(): Unit = {
    val dbTableDataProvider = new DBTableDataProvider(db.url, db.user, db.password)
    val results: QueryResultIterator = dbTableDataProvider.executeQuery(tableName, "column1", new Timestamp(0)) //0 is 1970-01-01 00:00:00

    var count = 0
    while (results.hasNext()) {
      val listBuffer: Seq[Object] = results.next()
      count = count + 1
    }
    count shouldBe 3
  }

  @Test
  def testDbCurrentTime(): Unit = {
    val dbTableDataProvider = new DBTableDataProvider(db.url, db.user, db.password)
    val dbCurrentTime = dbTableDataProvider.dbCurrentTime(DateTimeUtils.CALENDAR)
    val dbCurrentTimestamp = dbCurrentTime.getTime()
    val systemCurrentTimestamp = System.currentTimeMillis()
    ((systemCurrentTimestamp - dbCurrentTimestamp) < 5000) shouldBe true
  }

  @Test
  def testColumnList(): Unit = {
    val dbTableDataProvider = new DBTableDataProvider(db.url, db.user, db.password)
    val columns = dbTableDataProvider.columns(db.connection, tableName)
    columns(0) shouldBe "column1"
    columns(1) shouldBe "column2"
    columns(2) shouldBe "column3"
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(client)
    CloseOnce.close(db)
  }
}
