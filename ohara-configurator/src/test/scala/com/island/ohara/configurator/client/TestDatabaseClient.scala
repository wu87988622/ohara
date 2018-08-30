package com.island.ohara.configurator.client
import com.island.ohara.client.DatabaseClient
import com.island.ohara.integration.LocalDataBase
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.io.CloseOnce
import com.island.ohara.rule.MediumTest
import org.junit.{After, Test}
import org.scalatest.Matchers

class TestDatabaseClient extends MediumTest with Matchers {

  private[this] val db = LocalDataBase.mysql()

  private[this] val client = DatabaseClient(db.url, db.user, db.password)

  @Test
  def testDbName(): Unit = {
    client.name.toLowerCase shouldBe "mysql"
  }

  @Test
  def testList(): Unit = {
    val tableName = methodName
    val cf0 = RdbColumn("cf0", "INTEGER", true)
    val cf1 = RdbColumn("cf1", "INTEGER", false)
    val cf2 = RdbColumn("cf2", "INTEGER", false)
    client.createTable(tableName, Seq(cf2, cf0, cf1))
    val tables = client.tables(null, null, null)
    tables.size shouldBe 1
  }

  @Test
  def testCreate(): Unit = {
    val tableName = methodName
    val cf0 = RdbColumn("cf0", "INTEGER", true)
    val cf1 = RdbColumn("cf1", "INTEGER", false)
    val cf2 = RdbColumn("cf2", "INTEGER", false)
    val before = client.tables(null, null, null).size
    client.createTable(tableName, Seq(cf2, cf0, cf1))

    client.tables(null, null, null).size shouldBe 1 + before
    client.tables(null, null, null).count(_.name == tableName) shouldBe 1
    val cfs = client.tables(db.catalog, null, tableName).head.columns
    cfs.size shouldBe 3
    cfs.filter(_.name == "cf0").head.pk shouldBe true
    cfs.filter(_.name == "cf1").head.pk shouldBe false
    cfs.filter(_.name == "cf2").head.pk shouldBe false
  }

  @Test
  def testDrop(): Unit = {
    val tableName = methodName
    val cf0 = RdbColumn("cf0", "INTEGER", true)
    val cf1 = RdbColumn("cf1", "INTEGER", false)
    client.createTable(tableName, Seq(cf0, cf1))

    val before = client.tables(null, null, null).size
    client.dropTable(tableName)
    client.tables(null, null, null).size shouldBe before - 1
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(client)
    CloseOnce.close(db)
  }
}
