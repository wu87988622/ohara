package com.island.ohara.connector.jdbc.source

/**
  * This class is getting property value
  */
case class JDBCSourceConnectorConfig(dbURL: String,
                                     dbUserName: String,
                                     dbPassword: String,
                                     dbTableName: String,
                                     dbCatalogPattern: Option[String],
                                     dbSchemaPattern: Option[String],
                                     mode: String,
                                     timestampColumnName: String) {
  def toMap: Map[String, String] = Map(
    DB_URL -> dbURL,
    DB_USERNAME -> dbUserName,
    DB_PASSWORD -> dbPassword,
    DB_TABLENAME -> dbTableName,
    MODE -> mode,
    TIMESTAMP_COLUMN_NAME -> timestampColumnName
  ) ++ dbCatalogPattern.map(s => Map(DB_CATALOG_PATTERN -> s)).getOrElse(Map.empty) ++ dbSchemaPattern
    .map(s => Map(DB_SCHEMA_PATTERN -> s))
    .getOrElse(Map.empty)
}

object JDBCSourceConnectorConfig {
  def apply(props: Map[String, String]): JDBCSourceConnectorConfig = {
    JDBCSourceConnectorConfig(
      dbURL = props(DB_URL),
      dbUserName = props(DB_USERNAME),
      dbPassword = props(DB_PASSWORD),
      dbTableName = props(DB_TABLENAME),
      dbCatalogPattern = props.get(DB_CATALOG_PATTERN).filter(_.nonEmpty),
      dbSchemaPattern = props.get(DB_SCHEMA_PATTERN).filter(_.nonEmpty),
      mode = props.getOrElse(MODE, MODE_DEFAULT),
      timestampColumnName = props(TIMESTAMP_COLUMN_NAME)
    )
  }
}
