package com.island.ohara.integration

import java.util.function.Supplier
import org.junit.Test
import org.scalatest.Matchers
import com.island.ohara.client.util.CloseOnce._
import com.island.ohara.common.rule.MediumTest

class TestWorkers extends MediumTest with Matchers {

  @Test
  def testLocalMethod(): Unit = {
    an[IllegalArgumentException] should be thrownBy Workers.of(null, throw new IllegalArgumentException("you can't pass"))

    val connProps = "localhost:12345"
    val external = Workers.of(connProps, new Supplier[Brokers]() {
      override def get(): Brokers = throw new IllegalArgumentException("you can't pass")
    })
    try {
      external.connectionProps shouldBe connProps
      external.isLocal shouldBe false
    } finally external.close()

    doClose2(Zookeepers.of())(zk => Brokers.of(new Supplier[Zookeepers] {
      override def get(): Zookeepers = zk
    })) {
      case (zk, brokers) =>
        val local = Workers.of(new Supplier[Brokers] {
          override def get(): Brokers = brokers
        })
        try local.isLocal shouldBe true
        finally local.close()
    }
  }

  @Test
  def testRandomPort(): Unit = doClose3(Zookeepers.local(0))(Brokers.local(_, Array(0)))(Workers.local(_, Array(0))) {
    case (_, _, workers) => workers.connectionProps.split(",").head.split(":")(1).toInt should not be 0
  }
}
