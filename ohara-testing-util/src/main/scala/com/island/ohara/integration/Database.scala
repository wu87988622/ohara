package com.island.ohara.integration
import java.sql.{Connection, DriverManager}
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.client.util.CloseOnce
import com.island.ohara.common.util.CommonUtil
import com.wix.mysql.EmbeddedMysql.anEmbeddedMysql
import com.wix.mysql.config.Charset.UTF8
import com.wix.mysql.config.MysqldConfig.aMysqldConfig
import com.wix.mysql.distribution.Version.v5_7_latest

trait Database extends CloseOnce {
  def host: String
  def port: Int
  def databaseName: String
  def user: String
  def password: String
  def url: String
  def connection: Connection

  /**
    * @return true if this database is generated locally.
    */
  def isLocal: Boolean
}

object Database {
  private[integration] val DB_SERVER: String = "ohara.it.db"
  private[this] val COUNT = new AtomicInteger(0)

  private[integration] def parseString(dbString: String): (String, String, String, String, Int, String) = {
    // format => jdbc:db:user:password@//host:port/db_name
    if (!dbString.startsWith("jdbc")) throw new IllegalArgumentException(s"invalid url:$dbString")
    try {
      val dbInstance = dbString.split(":")(1)
      val user = dbString.split(":")(2)
      val password = dbString.split(":")(3).split("@//").head
      val host = dbString.split(":")(3).split("@//").last
      val port = dbString.split(":").last.split("/").head.toInt
      val dbName = dbString.split(":").last.split("/").last
      (dbInstance, user, password, host, port, dbName)
    } catch {
      case e: Throwable => throw new IllegalArgumentException(s"invalid value from $DB_SERVER", e)
    }
  }

  /**
    * create an embedded mysql with specific port
    * @param port bound port
    * @return an embedded mysql
    */
  def local(port: Int): Database = {
    val config = aMysqldConfig(v5_7_latest)
      .withCharset(UTF8)
      .withUser(s"user-${COUNT.getAndIncrement()}", s"password-${COUNT.getAndIncrement()}")
      .withTimeZone(CommonUtil.timezone)
      .withTimeout(2, TimeUnit.MINUTES)
      .withServerVariable("max_connect_errors", 666)
      .withTempDir(createTempDir("my_sql").getAbsolutePath)
      .withPort(if (port <= 0) availablePort() else port)
      // make mysql use " replace '
      // see https://stackoverflow.com/questions/13884854/mysql-double-quoted-table-names
      .withServerVariable("sql-mode", "ANSI_QUOTES")
      .build()
    val _dbName = s"db-${COUNT.getAndIncrement()}"
    val mysqld = anEmbeddedMysql(config).addSchema(_dbName).start()
    new Database {
      private[this] var _connection: Connection = _
      override def host: String = CommonUtil.hostname

      override def port: Int = config.getPort

      override def databaseName: String = _dbName

      override def user: String = config.getUsername

      override def password: String = config.getPassword

      override protected def doClose(): Unit = {
        CloseOnce.close(_connection)
        mysqld.stop()
      }

      override def url: String = s"jdbc:mysql://$host:$port/$databaseName"
      override def connection: Connection = {
        if (_connection == null) _connection = DriverManager.getConnection(url, user, password)
        _connection
      }
      override def isLocal: Boolean = true
    }
  }

  def apply(): Database = apply(sys.env.get(DB_SERVER))

  private[integration] def apply(db: Option[String]): Database = db
    .map { s =>
      val (dbInstance, _user, _password, _host, _port, dbName) = parseString(s)
      new Database {
        private[this] var _connection: Connection = _
        override def host: String = _host

        override def port: Int = _port

        override def databaseName: String = dbName

        override def user: String = _user

        override def password: String = _password

        override def url: String = s"jdbc:$dbInstance://$host:$port/$databaseName"

        override def connection: Connection = {
          if (_connection == null) _connection = DriverManager.getConnection(url, user, password)
          _connection
        }

        override protected def doClose(): Unit = CloseOnce.close(_connection)
        override def isLocal: Boolean = false
      }
    }
    .getOrElse(local(0))
}
