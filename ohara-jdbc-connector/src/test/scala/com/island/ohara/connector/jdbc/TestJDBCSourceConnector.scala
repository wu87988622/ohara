package com.island.ohara.connector.jdbc

import java.sql.Statement
import com.island.ohara.client.ConfiguratorJson.RdbColumn
import com.island.ohara.client.DatabaseClient
import com.island.ohara.connector.jdbc.source._
import com.island.ohara.data.{Cell, Row}
import com.island.ohara.integration.{LocalDataBase, With3Brokers3Workers}
import com.island.ohara.kafka.Consumer
import org.junit.{Before, Test}
import org.scalatest.Matchers
import scala.concurrent.duration._

/**
  * Test the JDBC Source Connector
  */
class TestJDBCSourceConnector extends With3Brokers3Workers with Matchers {
  private[this] val db = LocalDataBase.mysql()
  private[this] val client = DatabaseClient(db.url, db.user, db.password)
  private[this] val tableName = "table1"
  private[this] val timestampColumnName = "column1"
  private[this] val connectorClient = testUtil.connectorClient

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
  def testJDBCSourceConnector(): Unit = {
    val connectorName: String = "JDBC-Source-Connector-Test"
    val topicName: String = "topic-test-1"

    connectorClient
      .connectorCreator()
      .name(connectorName)
      .connectorClass(classOf[JDBCSourceConnector])
      .topic(topicName)
      .numberOfTasks(1)
      .config(props.toMap)
      .disableConverter()
      .create()

    val consumer =
      Consumer.builder().topicName(topicName).offsetFromBegin().brokers(testUtil.brokers).build[Array[Byte], Row]
    try {
      val record = consumer.poll(30 seconds, 3)

      val row0: Row = record(0).value.get
      row0.size shouldBe 4
      row0.cell(0).toString() shouldBe Cell("column1", "2018-09-01 00:00:00.0").toString()
      row0.cell(1) shouldBe Cell("column2", "a11")
      row0.cell(2) shouldBe Cell("column3", "a12")
      row0.cell(3).toString() shouldBe Cell("column4", "1").toString()
    } finally consumer.close()
  }

  private[this] val props = JDBCSourceConnectorConfig(
    Map(DB_URL -> db.url,
        DB_USERNAME -> db.user,
        DB_PASSWORD -> db.password,
        DB_TABLENAME -> tableName,
        TIMESTAMP_COLUMN_NAME -> timestampColumnName,
        DB_SCHEMA_PATTERN -> ""))
}
