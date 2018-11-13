package com.island.ohara.it

import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.DatabaseClient
import com.island.ohara.client.util.CloseOnce
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.integration.Database
import org.junit.{After, Test}
import org.scalatest.Matchers

class TestDatabaseClient extends MediumTest with Matchers {

  private[this] val db = Database()

  private[this] val client = DatabaseClient(db.url, db.user, db.password)

  private[this] val increasedNumber = client.name match {
    // postgresql generate one table called "xxx_pkey"
    case "postgresql" => 2
    case _            => 1
  }
  @Test
  def testList(): Unit = {
    val before = client.tables(null, null, null).size
    val tableName = methodName
    val cf0 = RdbColumn("cf0", "INTEGER", true)
    val cf1 = RdbColumn("cf1", "INTEGER", false)
    val cf2 = RdbColumn("cf2", "INTEGER", false)
    client.createTable(tableName, Seq(cf2, cf0, cf1))
    try {
      val after = client.tables(null, null, null).size
      after - before shouldBe increasedNumber
    } finally client.dropTable(tableName)
  }

  @Test
  def testCreate(): Unit = {
    // postgresql use lower case...
    val tableName = methodName
    val cf0 = RdbColumn("cf0", "INTEGER", true)
    val cf1 = RdbColumn("cf1", "INTEGER", true)
    val cf2 = RdbColumn("cf2", "INTEGER", false)
    val before = client.tables(null, null, null).size
    client.createTable(tableName, Seq(cf2, cf0, cf1))
    try {
      client.tables(null, null, null).size - before shouldBe increasedNumber
      val cfs = client.tables(null, null, tableName).head.schema
      cfs.size shouldBe 3
      cfs.filter(_.name == "cf0").head.pk shouldBe true
      cfs.filter(_.name == "cf1").head.pk shouldBe true
      cfs.filter(_.name == "cf2").head.pk shouldBe false
    } finally client.dropTable(tableName)
  }

  @Test
  def testDrop(): Unit = {
    val tableName = methodName
    val cf0 = RdbColumn("cf0", "INTEGER", true)
    val cf1 = RdbColumn("cf1", "INTEGER", false)
    client.createTable(tableName, Seq(cf0, cf1))
    val before = client.tables(null, null, null).size
    client.dropTable(tableName)
    before - client.tables(null, null, null).size shouldBe increasedNumber
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(client)
    CloseOnce.close(db)
  }
}
