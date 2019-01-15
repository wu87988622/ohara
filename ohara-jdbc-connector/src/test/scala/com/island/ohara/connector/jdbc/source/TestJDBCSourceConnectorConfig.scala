package com.island.ohara.connector.jdbc.source
import com.island.ohara.common.rule.SmallTest
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
    jdbcSourceConnectorConfig.dbSchemaPattern.get shouldBe "schema1"
    jdbcSourceConnectorConfig.timestampColumnName shouldBe "CDC_TIMESTAMP"
  }

  @Test
  def testException(): Unit = {
    intercept[NoSuchElementException] {
      JDBCSourceConnectorConfig(Map())
    }.getMessage shouldBe s"key not found: $DB_URL"

    intercept[NoSuchElementException] {
      JDBCSourceConnectorConfig(Map(DB_URL -> "jdbc:mysql://localhost:3306"))
    }.getMessage shouldBe s"key not found: $DB_USERNAME"

    intercept[NoSuchElementException] {
      JDBCSourceConnectorConfig(Map(DB_URL -> "jdbc:mysql://localhost/test", DB_USERNAME -> "root"))
    }.getMessage shouldBe s"key not found: $DB_PASSWORD"
  }

  @Test
  def testCatalogAndSchema(): Unit = {
    val config = JDBCSourceConnectorConfig(
      dbURL = "123",
      dbUserName = "123",
      dbPassword = "123",
      dbTableName = "123",
      dbCatalogPattern = None,
      dbSchemaPattern = None,
      mode = "123",
      timestampColumnName = "123"
    )

    config.toMap.contains(DB_CATALOG_PATTERN) shouldBe false
    config.toMap.contains(DB_SCHEMA_PATTERN) shouldBe false

    val configMap = Map[String, String](
      DB_URL -> "aa",
      DB_USERNAME -> "aa",
      DB_PASSWORD -> "aa",
      DB_TABLENAME -> "aa",
      DB_CATALOG_PATTERN -> "aa",
      DB_SCHEMA_PATTERN -> "aa",
      MODE -> "aa",
      TIMESTAMP_COLUMN_NAME -> "aa"
    )

    JDBCSourceConnectorConfig(configMap).dbSchemaPattern.isEmpty shouldBe false
    JDBCSourceConnectorConfig(configMap).dbCatalogPattern.isEmpty shouldBe false

    val configMap2 = Map[String, String](
      DB_URL -> "aa",
      DB_USERNAME -> "aa",
      DB_PASSWORD -> "aa",
      DB_TABLENAME -> "aa",
      MODE -> "aa",
      TIMESTAMP_COLUMN_NAME -> "aa"
    )

    JDBCSourceConnectorConfig(configMap2).dbSchemaPattern.isEmpty shouldBe true
    JDBCSourceConnectorConfig(configMap2).dbCatalogPattern.isEmpty shouldBe true

    JDBCSourceConnectorConfig(configMap2) shouldBe JDBCSourceConnectorConfig(configMap2)
    JDBCSourceConnectorConfig(configMap2) shouldBe JDBCSourceConnectorConfig(
      JDBCSourceConnectorConfig(configMap2).toMap)
  }
}
