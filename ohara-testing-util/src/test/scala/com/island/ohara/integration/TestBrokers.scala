package com.island.ohara.integration
import org.junit.Test
import org.scalatest.Matchers
import com.island.ohara.client.util.CloseOnce._
import com.island.ohara.common.rule.MediumTest
class TestBrokers extends MediumTest with Matchers {

  @Test
  def testLocalMethod(): Unit = {
    an[IllegalArgumentException] should be thrownBy Brokers(None, throw new IllegalArgumentException("you can't pass"))

    val connProps = "localhost:12345"
    val external = Brokers(Some(connProps), throw new IllegalArgumentException("you can't pass"))
    try {
      external.connectionProps shouldBe connProps
      external.isLocal shouldBe false
    } finally external.close()

    val zk = Zookeepers()
    try {
      val local = Brokers(None, zk)
      try local.isLocal shouldBe true
      finally local.close()
    } finally zk.close()
  }

  @Test
  def testRandomPort(): Unit = doClose2(Zookeepers.local(0))(Brokers.local(_, Seq(0))) {
    case (_, brokers) => brokers.connectionProps.split(",").head.split(":")(1).toInt should not be 0
  }
}
