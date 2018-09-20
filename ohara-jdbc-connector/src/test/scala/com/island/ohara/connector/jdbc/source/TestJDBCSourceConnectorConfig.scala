package com.island.ohara.connector.jdbc.source

import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestJDBCSourceConnectorConfig extends SmallTest with Matchers {

  @Test
  def testSettingProperty(): Unit = {
    val map1: Map[String, String] =
      Map(
        DB_URL -> "jdbc:mysql://localhost/test",
        DB_USERNAME -> "root",
        DB_PASSWORD -> "123456",
        DB_TABLENAME -> "TABLE1",
        DB_SCHEMA_PATTERN -> "schema1",
        TIMESTAMP_COLUMN_NAME -> "CDC_TIMESTAMP"
      )

    val jdbcSourceConnectorConfig = JDBCSourceConnectorConfig(map1)
    jdbcSourceConnectorConfig.dbURL shouldBe "jdbc:mysql://localhost/test"
    jdbcSourceConnectorConfig.dbUserName shouldBe "root"
    jdbcSourceConnectorConfig.dbPassword shouldBe "123456"
    jdbcSourceConnectorConfig.dbTableName shouldBe "TABLE1"
    jdbcSourceConnectorConfig.mode shouldBe "timestamp"
    jdbcSourceConnectorConfig.dbSchemaPattern shouldBe "schema1"
    jdbcSourceConnectorConfig.timestampColumnName shouldBe "CDC_TIMESTAMP"
  }

  @Test
  def testException(): Unit = {
    intercept[NoSuchElementException] {
      JDBCSourceConnectorConfig(Map())
    }.getMessage() shouldBe s"key not found: $DB_URL"

    intercept[NoSuchElementException] {
      JDBCSourceConnectorConfig(Map(DB_URL -> "jdbc:mysql://localhost:3306"))
    }.getMessage() shouldBe s"key not found: $DB_USERNAME"

    intercept[NoSuchElementException] {
      JDBCSourceConnectorConfig(Map(DB_URL -> "jdbc:mysql://localhost/test", DB_USERNAME -> "root"))
    }.getMessage() shouldBe s"key not found: $DB_PASSWORD"
  }
}
