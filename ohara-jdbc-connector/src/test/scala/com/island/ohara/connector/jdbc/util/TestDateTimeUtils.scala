package com.island.ohara.connector.jdbc.util

import java.sql.Timestamp
import com.island.ohara.rule.MediumTest
import org.junit.Test
import org.scalatest.Matchers

class TestDateTimeUtils extends MediumTest with Matchers {

  @Test
  def testTaipeiTimeZone(): Unit = {
    System.setProperty("user.timezone", "Asia/Taipei")
    val timestamp: Timestamp = new Timestamp(0)
    timestamp.toString() shouldBe "1970-01-01 08:00:00.0"
  }
}
