package com.island.ohara.configurator
import java.util.concurrent.{Executors, TimeUnit}

import com.island.ohara.common.rule.LargeTest
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.integration.OharaTestUtil
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.{ExecutionContext, Future}

class TestConfiguratorMain extends LargeTest with Matchers {

  @Test
  def testInvalidInputs(): Unit = {
    an[IllegalArgumentException] should be thrownBy Configurator.main(
      Array[String](
        Configurator.HOSTNAME_KEY,
        "localhost",
        Configurator.PORT_KEY,
        "0",
        Configurator.BROKERS_KEY,
        "abc"
      ))
  }

  @Test
  def testStandalone(): Unit = {
    Configurator.closeRunningConfigurator = false
    val service = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
    Future[Unit] {
      Configurator.main(Array[String](Configurator.HOSTNAME_KEY, "localhost", Configurator.PORT_KEY, "0"))
    }(service)
    import java.time.Duration
    try CommonUtil.await(() => Configurator.hasRunningConfigurator, Duration.ofSeconds(20))
    finally {
      Configurator.closeRunningConfigurator = true
      service.shutdownNow()
      service.awaitTermination(60, TimeUnit.SECONDS)
    }
  }

  @Test
  def testActualEnv(): Unit = {
    val util = OharaTestUtil.workers(1)
    try {
      Configurator.closeRunningConfigurator = false
      val service = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
      Future[Unit] {
        Configurator.main(
          Array[String](
            Configurator.HOSTNAME_KEY,
            "localhost",
            Configurator.PORT_KEY,
            "0",
            Configurator.BROKERS_KEY,
            util.brokersConnProps,
            Configurator.WORKERS_KEY,
            util.workersConnProps
          ))
      }(service)
      import java.time.Duration
      try CommonUtil.await(() => Configurator.hasRunningConfigurator, Duration.ofSeconds(30))
      finally {
        Configurator.closeRunningConfigurator = true
        service.shutdownNow()
        service.awaitTermination(60, TimeUnit.SECONDS)
      }
    } finally util.close()
  }
}
