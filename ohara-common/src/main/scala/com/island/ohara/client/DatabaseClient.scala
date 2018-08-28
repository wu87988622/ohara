package com.island.ohara.client
import java.sql.{DriverManager, ResultSet}

import com.island.ohara.client.ConfiguratorJson.RdbTable
import com.island.ohara.io.CloseOnce

import scala.collection.mutable.ArrayBuffer

/**
  * a easy database client.
  */
trait DatabaseClient extends CloseOnce {

  /**
    * @param catalog catalog
    * @param name table name
    * @return a list of table's information
    */
  def tables(catalog: String, name: String): Seq[RdbTable]

  /**
    * @return a list of table's information
    */
  def tables(): Seq[RdbTable] = tables(null, null)

  /**
    * assert the singleton table with specified catalog and name
    */
  def table(catalog: String, name: String): RdbTable = {
    val alternatives = tables(catalog, name)
    alternatives.size match {
      case 0 => throw new IllegalArgumentException(s"$catalog:$name doesn't exist")
      case 1 => alternatives.iterator.next()
      case _ => throw new IllegalArgumentException(s"$catalog:$name have duplicate tables...???")
    }
  }

  def name: String

  def createTable(name: String, columns: Seq[ConfiguratorJson.RdbColumn]): Unit

  def dropTable(name: String): Unit

  def closed: Boolean
}

object DatabaseClient {
  private[this] def tableCatalog(implicit rs: ResultSet): String = rs.getString("TABLE_CAT")
  private[this] def tableName(implicit rs: ResultSet): String = rs.getString("TABLE_NAME")
  private[this] def columnName(implicit rs: ResultSet): String = rs.getString("COLUMN_NAME")
  private[this] def columnType(implicit rs: ResultSet): String = rs.getString("TYPE_NAME")
  private[this] def tableType(implicit rs: ResultSet): Seq[String] = {
    val r = rs.getString("TABLE_TYPE")
    if (r == null) Seq.empty
    else r.split(" ")
  }
  private[this] def systemTable(types: Seq[String]): Boolean = !types.filter(_.equals("SYSTEM")).isEmpty

  /**
    * @return a jdbc-based DatabaseClient
    */
  def apply(url: String, user: String, password: String): DatabaseClient = new DatabaseClient {
    private[this] val conn = DriverManager.getConnection(url, user, password)

    override def closed: Boolean = conn.isClosed

    def tables(catalog: String, name: String): Seq[RdbTable] = {
      val md = conn.getMetaData

      CloseOnce
        .doClose(md.getTables(catalog, null, if (name == null) "%" else name, null)) { implicit rs =>
          {
            val buf = new ArrayBuffer[(String, String)]()
            while (rs.next()) if (!systemTable(tableType)) buf.append((tableCatalog, tableName))
            buf
          }
        }
        .map {
          case (c, t) => {
            (c, t, CloseOnce.doClose(md.getPrimaryKeys(c, null, t)) { implicit rs =>
              {
                val buf = new ArrayBuffer[String]()
                while (rs.next()) buf += columnName
                buf.toSet
              }
            })
          }
        }
        .map {
          case (c, t, pks) => {
            val columns = CloseOnce.doClose(md.getColumns(c, null, t, null)) { implicit rs =>
              {
                val buf = new ArrayBuffer[ConfiguratorJson.RdbColumn]()
                while (rs.next()) {
                  buf += ConfiguratorJson.RdbColumn(name = columnName,
                                                    typeName = columnType,
                                                    pk = pks.contains(columnName))
                }
                buf
              }
            }
            RdbTable(c, t, columns)
          }
        }
        .filterNot(_.columns.isEmpty)
    }

    override protected def doClose(): Unit = conn.close()

    override def name: String = {
      val l = url.indexOf(":")
      if (l < 0) return url
      val r = url.indexOf(":", l + 1)
      if (r < 0) return url
      url.substring(l + 1, r)
    }
    override def createTable(name: String, columns: Seq[ConfiguratorJson.RdbColumn]): Unit = {
      if (columns.map(_.name).toSet.size != columns.size) throw new IllegalArgumentException(s"duplicate order!!!")
      val query = s"CREATE TABLE $name (" + columns
        .map(c => {
          s"${c.name} ${c.typeName}"
        })
        .mkString(",") + ", PRIMARY KEY (" + columns.filter(_.pk).map(_.name).mkString(",") + "))"
      CloseOnce.doClose(conn.createStatement()) { state =>
        state.execute(query)
      }
    }
    override def dropTable(name: String): Unit = {
      val query = s"DROP TABLE $name"
      CloseOnce.doClose(conn.createStatement()) { state =>
        state.execute(query)
      }
    }
  }
}
