package com.island.ohara.integration
import com.island.ohara.rule.MediumTest
import org.junit.Test
import org.scalatest.Matchers

class TestZookeepers extends MediumTest with Matchers {

  @Test
  def testLocalMethod(): Unit = {
    val url = "localhost:12345"
    val external = Zookeepers(Some(url))
    try {
      external.isLocal shouldBe false
      external.connectionProps shouldBe url
    } finally external.close()

    val local = Zookeepers()
    try local.isLocal shouldBe true
    finally local.close()
  }
}
