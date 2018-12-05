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
    val host = "hostname"
    val port = 123
    val dbName = "dbName"

    val result = Database.parseString(s"jdbc:$dbInstance:$user:$password@//$host:$port/$dbName")
    result.getDbInstance() shouldBe dbInstance
    result.getUser() shouldBe user
    result.getPassword() shouldBe password
    result.getHost() shouldBe host
    result.getPort() shouldBe port
    result.getDbName() shouldBe dbName

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
    val host = "hostname"
    val port = 123
    val dbName = "dbName"

    //val externaldb = Database(Some(s"jdbc:$dbInstance:$user:$password@//$host:$port/$dbName"))
    val externaldb = Database.of("jdbc:" + dbInstance + ":" + user + ":" + password + "@//" + host + ":" + port + "/" + dbName)
    try {
      externaldb.isLocal shouldBe false
      externaldb.user shouldBe user
      externaldb.password shouldBe password
      externaldb.hostname shouldBe host
      externaldb.port shouldBe port
      externaldb.databaseName shouldBe dbName
    } finally externaldb.close()

    val localdb = Database.of()
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
