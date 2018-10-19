package com.island.ohara.integration

import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestExternalSystemProps extends SmallTest with Matchers {

  @Test
  def testFtpServer(): Unit = {
    val user = "user"
    val password = "password"
    val host = "host"
    val port = 123

    val result = FtpServer.parseString(s"$user:$password@$host:$port")
    result._1 shouldBe user
    result._2 shouldBe password
    result._3 shouldBe host
    result._4 shouldBe port

    // a random string
    an[IllegalArgumentException] should be thrownBy FtpServer.parseString("adadasdasd")
  }

  @Test
  def testDb(): Unit = {
    val dbInstance = "mysql"
    val user = "user"
    val password = "password"
    val host = "host"
    val port = 123
    val dbName = "dbName"

    val result = DataBase.parseString(s"jdbc:$dbInstance:$user:$password@//$host:$port/$dbName")
    result._1 shouldBe dbInstance
    result._2 shouldBe user
    result._3 shouldBe password
    result._4 shouldBe host
    result._5 shouldBe port
    result._6 shouldBe dbName

    // the string should start with "jdbc"
    an[IllegalArgumentException] should be thrownBy DataBase.parseString(
      s"abc:$dbInstance:$user:$password@//$host:$port/$dbName")

    // a random string
    an[IllegalArgumentException] should be thrownBy DataBase.parseString("adadasdasd")
  }
}
