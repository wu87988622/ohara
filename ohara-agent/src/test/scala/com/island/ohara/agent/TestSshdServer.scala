package com.island.ohara.agent

import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestSshdServer extends SmallTest with Matchers {

  @Test
  def testParseString(): Unit = {
    val user = "aaa"
    val password = "ccc"
    val hostname = "aaaaa"
    val port = 12345
    val server = SshdServer.parseString(s"$user:$password@$hostname:$port")
    server._1 shouldBe user
    server._2 shouldBe password
    server._3 shouldBe hostname
    server._4 shouldBe port
  }

  @Test
  def testApply(): Unit = {
    val user = "aaa"
    val password = "ccc"
    val hostname = "aaaaa"
    val port = 12345
    val server = SshdServer(Some(s"$user:$password@$hostname:$port"))
    try {
      server.user shouldBe user
      server.password shouldBe password
      server.hostname shouldBe hostname
      server.port shouldBe port
    } finally server.close()
  }

  @Test
  def testPort(): Unit = {
    val server = SshdServer.local(0)
    try {
      server.port should not be 0
    } finally server.close()
  }
}
