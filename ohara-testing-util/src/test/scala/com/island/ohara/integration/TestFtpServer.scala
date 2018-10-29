package com.island.ohara.integration

import com.island.ohara.rule.MediumTest
import org.junit.Test
import org.scalatest.Matchers

class TestFtpServer extends MediumTest with Matchers {

  @Test
  def testExternalFtpServer(): Unit = {
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
  def testLocalMethod(): Unit = {
    val user = "user"
    val password = "password"
    val host = "host"
    val port = 123
    val externalFtpServer = FtpServer(Some(s"$user:$password@$host:$port"))
    try {
      externalFtpServer.isLocal shouldBe false
      externalFtpServer.user shouldBe user
      externalFtpServer.password shouldBe password
      externalFtpServer.host shouldBe host
      externalFtpServer.port shouldBe port
    } finally externalFtpServer.close()

    val localFtpServer = FtpServer()
    try localFtpServer.isLocal shouldBe true
    finally localFtpServer.close()
  }

  @Test
  def testRandomPort(): Unit = {
    val ftpServer = FtpServer.local(0)
    try ftpServer.port should not be 0
    finally ftpServer.close()
  }
}
