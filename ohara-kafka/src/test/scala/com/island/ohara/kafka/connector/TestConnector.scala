package com.island.ohara.kafka.connector
import com.island.ohara.rule.SmallTest
import com.island.ohara.util.VersionUtil
import org.junit.Test
import org.scalatest.Matchers

class TestConnector extends SmallTest with Matchers {

  /**
    * this test is used to prevent us from breaking the format of version exposed to kafka connector
    */
  @Test
  def testVersion(): Unit = {
    VERSION shouldBe s"${VersionUtil.VERSION}_${VersionUtil.REVISION}"
  }
}
