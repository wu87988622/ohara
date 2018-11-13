package com.island.ohara.demo
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestBackendArguments extends SmallTest with Matchers {

  @Test
  def ftpPortRangeShouldWork(): Unit = {
    val dataPorts = 12345 to 12350
    val (_, ports) = Backend.parse(Array(Backend.FTP_DATA_PORT_KEY, s"${dataPorts.head}-${dataPorts.last}"))
    ports.ftpDataPorts shouldBe dataPorts
  }

  @Test
  def brokersPortRangeShouldWork(): Unit = {
    val dataPorts = 12345 to 12350
    val (_, ports) = Backend.parse(Array(Backend.BROKERS_PORT_KEY, s"${dataPorts.head}-${dataPorts.last}"))
    ports.brokersPort shouldBe dataPorts
  }

  @Test
  def workersPortRangeShouldWork(): Unit = {
    val dataPorts = 12345 to 12350
    val (_, ports) = Backend.parse(Array(Backend.WORKERS_PORT_KEY, s"${dataPorts.head}-${dataPorts.last}"))
    ports.workersPort shouldBe dataPorts
  }
}
