package com.island.ohara.integration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.io.CloseOnce
import com.wix.mysql.EmbeddedMysql.anEmbeddedMysql
import com.wix.mysql.config.Charset.UTF8
import com.wix.mysql.config.MysqldConfig.aMysqldConfig
import com.wix.mysql.distribution.Version.v5_7_latest

trait LocalDataBase extends CloseOnce {
  def host: String
  def port: Int
  def dbName: String
  def user: String
  def password: String
  def url: String
}

object LocalDataBase {
  private[this] val COUNT = new AtomicInteger(0)

  def mysql(): LocalDataBase = {
    val config = aMysqldConfig(v5_7_latest)
      .withCharset(UTF8)
      .withUser(s"user-${COUNT.getAndIncrement()}", s"password-${COUNT.getAndIncrement()}")
      .withTimeZone("Asia/Taipei")
      .withTimeout(2, TimeUnit.MINUTES)
      .withServerVariable("max_connect_errors", 666)
      .withFreePort()
      .withTempDir(createTempDir("my_sql").getAbsolutePath)
      .build()

    val _dbName = s"db-${COUNT.getAndIncrement()}"
    val mysqld = anEmbeddedMysql(config).addSchema(_dbName).start()
    new LocalDataBase {
      override def host: String = "localhost"

      override def port: Int = config.getPort

      override def dbName: String = _dbName

      override def user: String = config.getUsername

      override def password: String = config.getPassword

      override protected def doClose(): Unit = mysqld.stop()

      override def url: String = s"jdbc:mysql://$host:$port/$dbName"
    }
  }
}
