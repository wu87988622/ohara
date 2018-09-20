package com.island.ohara.connector.jdbc

package object source {
  val DB_URL: String = "source.db.url"
  val DB_USERNAME: String = "source.db.username"
  val DB_PASSWORD: String = "source.db.password"
  val DB_TABLENAME: String = "source.table.name"
  val DB_SCHEMA_PATTERN: String = "source.schema.pattern"
  val MODE: String = "mode"
  val MODE_DEFAULT = "timestamp"
  val TIMESTAMP_COLUMN_NAME: String = "source.timestamp.column.name"
}
