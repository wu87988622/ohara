package com.island.ohara.integration
import java.util.function.Supplier
import com.island.ohara.client.util.CloseOnce._
import org.junit.Test
import org.scalatest.Matchers
import com.island.ohara.common.rule.MediumTest

class TestBrokers extends MediumTest with Matchers {

  @Test
  def testLocalMethod(): Unit = {
    an[IllegalArgumentException] should be thrownBy Brokers.of(null, throw new IllegalArgumentException("you can't pass"))

    val connProps = "localhost:12345"

    val external = Brokers.of(connProps, new Supplier[Zookeepers] {
      override def get(): Zookeepers = throw new IllegalArgumentException("you can't pass")
    });

    try {
      external.connectionProps() shouldBe connProps
      external.isLocal shouldBe false
    } finally external.close()

    val zk = Zookeepers.of()
    try {
      val local = Brokers.of(new Supplier[Zookeepers] {
        override def get(): Zookeepers = zk
      })
      try local.isLocal shouldBe true
      finally local.close()
    } finally zk.close()
  }

  @Test
  def testRandomPort(): Unit = doClose2(Zookeepers.local(0))(Brokers.local(_, Array(0))) {
    case (_, brokers) => brokers.connectionProps.split(",").head.split(":")(1).toInt should not be 0
  }
}
