package com.island.ohara.integration

import com.island.ohara.rule.MediumTest
import org.junit.Test
import org.scalatest.Matchers
import com.island.ohara.io.CloseOnce._

class TestWorkers extends MediumTest with Matchers {

  @Test
  def testLocalMethod(): Unit = {
    an[IllegalArgumentException] should be thrownBy Workers(None, throw new IllegalArgumentException("you can't pass"))

    val connProps = "localhost:12345"
    val external = Workers(Some(connProps), throw new IllegalArgumentException("you can't pass"))
    try {
      external.connectionProps shouldBe connProps
      external.isLocal shouldBe false
    } finally external.close()

    doClose2(Zookeepers())(zk => Brokers(zk)) {
      case (zk, brokers) =>
        val local = Workers(brokers)
        try local.isLocal shouldBe true
        finally local.close()
    }
  }
  @Test
  def testRandomPort(): Unit = doClose3(Zookeepers.local(0))(Brokers.local(_, Seq(0)))(Workers.local(_, Seq(0))) {
    case (_, _, workers) => workers.connectionProps.split(",").head.split(":")(1).toInt should not be 0
  }
}
