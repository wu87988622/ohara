package com.island.ohara.connector.jdbc.source

/**
  * This class is getting property value
  */
case class JDBCSourceConnectorConfig(dbURL: String,
                                     dbUserName: String,
                                     dbPassword: String,
                                     dbTableName: String,
                                     dbSchemaPattern: String,
                                     mode: String,
                                     timestampColumnName: String) {
  def toMap: Map[String, String] = Map(
    DB_URL -> dbURL,
    DB_USERNAME -> dbUserName,
    DB_PASSWORD -> dbPassword,
    DB_TABLENAME -> dbTableName,
    DB_SCHEMA_PATTERN -> dbSchemaPattern,
    MODE -> mode,
    TIMESTAMP_COLUMN_NAME -> timestampColumnName
  )
}

object JDBCSourceConnectorConfig {
  def apply(props: Map[String, String]): JDBCSourceConnectorConfig = {
    JDBCSourceConnectorConfig(
      dbURL = props(DB_URL),
      dbUserName = props(DB_USERNAME),
      dbPassword = props(DB_PASSWORD),
      dbTableName = props(DB_TABLENAME),
      dbSchemaPattern = props(DB_SCHEMA_PATTERN),
      mode = props.getOrElse(MODE, MODE_DEFAULT),
      timestampColumnName = props(TIMESTAMP_COLUMN_NAME)
    )
  }
}
