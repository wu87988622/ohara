package com.island.ohara.integration

import java.util.function.Supplier

import com.island.ohara.common.rule.MediumTest
import org.junit.Test
import org.scalatest.Matchers

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

    val zk = Zookeepers.of()
    try {
      val brokers = Brokers.of(new Supplier[Zookeepers] {
        override def get(): Zookeepers = zk
      })
      val local = Workers.of(new Supplier[Brokers] {
        override def get(): Brokers = brokers
      })
      try local.isLocal shouldBe true
      finally local.close()
    } finally zk.close()
  }

  @Test
  def testRandomPort(): Unit = {
    val zk = Zookeepers.local(0)
    try {
      val brokers = Brokers.local(zk, Array(0))
      try {
        val workers = Workers.local(brokers, Array(0))
        try workers.connectionProps.split(",").head.split(":")(1).toInt should not be 0 finally workers.close()
      } finally brokers.close()
    } finally zk.close()
  }
}
