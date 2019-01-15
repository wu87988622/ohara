package com.island.ohara.client
import java.sql.{Connection, DriverManager, ResultSet}

import com.island.ohara.client.configurator.v0.QueryApi.{RdbColumn, RdbTable}
import com.island.ohara.common.util.ReleaseOnce

import scala.collection.mutable.ArrayBuffer

/**
  * a easy database client.
  */
trait DatabaseClient extends ReleaseOnce {

  /**
    * @param catalog catalog
    * @param name table name
    * @return a list from table's information
    */
  def tables(catalog: String, schema: String, name: String): Seq[RdbTable]

  def name: String

  def createTable(name: String, schema: Seq[RdbColumn]): Unit

  def dropTable(name: String): Unit

  def closed: Boolean

  def connection: Connection
}

object DatabaseClient {
  private[this] def tableCatalog(implicit rs: ResultSet): String = rs.getString("TABLE_CAT")
  private[this] def tableSchema(implicit rs: ResultSet): String = rs.getString("TABLE_SCHEM")
  private[this] def tableName(implicit rs: ResultSet): String = rs.getString("TABLE_NAME")
  private[this] def columnName(implicit rs: ResultSet): String = rs.getString("COLUMN_NAME")
  private[this] def columnType(implicit rs: ResultSet): String = rs.getString("TYPE_NAME")
  private[this] def tableType(implicit rs: ResultSet): Seq[String] = {
    val r = rs.getString("TABLE_TYPE")
    if (r == null) Seq.empty
    else r.split(" ")
  }
  private[this] def systemTable(types: Seq[String]): Boolean = types.contains("SYSTEM")

  /**
    * @return a jdbc-based DatabaseClient
    */
  def apply(url: String, user: String, password: String): DatabaseClient = new DatabaseClient {

    private[this] val conn = DriverManager.getConnection(url, user, password)

    override def closed: Boolean = conn.isClosed

    override def tables(catalog: String, schema: String, name: String): Seq[RdbTable] = {
      val md = conn.getMetaData

      // catalog, schema, tableName
      val data: Seq[(String, String, String)] = {
        implicit val rs: ResultSet = md.getTables(catalog, schema, name, null)
        try {
          val buf = new ArrayBuffer[(String, String, String)]()
          while (rs.next()) if (!systemTable(tableType)) buf.append((tableCatalog, tableSchema, tableName))
          buf
        } finally rs.close()
      }

      // catalog, schema, tableName, pks
      val data2 = data.map {
        case (c, s, t) =>
          (c, s, t, {
            implicit val rs: ResultSet = md.getPrimaryKeys(c, null, t)
            try {
              val buf = new ArrayBuffer[String]()
              while (rs.next()) buf += columnName
              buf.toSet
            } finally rs.close()
          })
      }

      data2
        .map {
          case (c, s, t, pks) =>
            implicit val rs: ResultSet = md.getColumns(c, null, t, null)
            val columns = try {
              val buf = new ArrayBuffer[RdbColumn]()
              while (rs.next()) buf += RdbColumn(name = columnName,
                                                 dataType = columnType,
                                                 pk = pks.contains(columnName))
              buf
            } finally rs.close()
            RdbTable(Option(c), Option(s), t, columns)
        }
        .filterNot(_.schema.isEmpty)
    }

    override protected def doClose(): Unit = conn.close()

    override def name: String = {
      val l = url.indexOf(":")
      if (l < 0) return url
      val r = url.indexOf(":", l + 1)
      if (r < 0) return url
      url.substring(l + 1, r)
    }
    override def createTable(name: String, columns: Seq[RdbColumn]): Unit =
      if (columns.map(_.name).toSet.size != columns.size)
        throw new IllegalArgumentException(s"duplicate order!!!")
      else
        execute(s"""CREATE TABLE \"$name\" (""" + columns
          .map(c => s"""\"${c.name}\" ${c.dataType}""")
          .mkString(",") + ", PRIMARY KEY (" + columns.filter(_.pk).map(c => s"""\"${c.name}\"""").mkString(",") + "))")

    override def dropTable(name: String): Unit = execute(s"""DROP TABLE \"$name\"""")

    private[this] def execute(query: String): Unit = {
      val state = conn.createStatement()
      try state.execute(query)
      finally state.close()
    }

    override def connection: Connection = conn
  }
}
