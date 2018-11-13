package com.island.ohara.integration
import com.island.ohara.common.rule.MediumTest
import org.junit.Test
import org.scalatest.Matchers

class TestDatabase extends MediumTest with Matchers {

  @Test
  def testExternalDb(): Unit = {
    val dbInstance = "mysql"
    val user = "user"
    val password = "password"
    val host = "host"
    val port = 123
    val dbName = "dbName"

    val result = Database.parseString(s"jdbc:$dbInstance:$user:$password@//$host:$port/$dbName")
    result._1 shouldBe dbInstance
    result._2 shouldBe user
    result._3 shouldBe password
    result._4 shouldBe host
    result._5 shouldBe port
    result._6 shouldBe dbName

    // the string should start with "jdbc"
    an[IllegalArgumentException] should be thrownBy Database.parseString(
      s"abc:$dbInstance:$user:$password@//$host:$port/$dbName")

    // a random string
    an[IllegalArgumentException] should be thrownBy Database.parseString("adadasdasd")
  }

  @Test
  def testLocalMethod(): Unit = {
    val dbInstance = "mysql"
    val user = "user"
    val password = "password"
    val host = "host"
    val port = 123
    val dbName = "dbName"

    val externaldb = Database(Some(s"jdbc:$dbInstance:$user:$password@//$host:$port/$dbName"))
    try {
      externaldb.isLocal shouldBe false
      externaldb.user shouldBe user
      externaldb.password shouldBe password
      externaldb.host shouldBe host
      externaldb.port shouldBe port
      externaldb.databaseName shouldBe dbName
    } finally externaldb.close()

    val localdb = Database()
    try localdb.isLocal shouldBe true
    finally localdb.close()
  }

  @Test
  def testRandomPort(): Unit = {
    val db = Database.local(0)
    try db.port should not be 0
    finally db.close()
  }
}
