package com.island.ohara.integration
import com.island.ohara.rule.MediumTest
import org.junit.Test
import org.scalatest.Matchers

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
}
