package com.island.ohara.configurator
import java.util.concurrent.{Executors, TimeUnit}

import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.client.util.CloseOnce.doClose
import com.island.ohara.common.rule.LargeTest
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
    import scala.concurrent.duration._
    try OharaTestUtil.await(() => Configurator.hasRunningConfigurator, 10 seconds)
    finally {
      Configurator.closeRunningConfigurator = true
      service.shutdownNow()
      service.awaitTermination(60, TimeUnit.SECONDS)
    }
  }

  @Test
  def testActualEnv(): Unit = {
    doClose(OharaTestUtil.workers()) { util =>
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
            util.workersConnProps,
            Configurator.TOPIC_KEY,
            methodName
          ))
      }(service)
      import scala.concurrent.duration._
      try OharaTestUtil.await(() => Configurator.hasRunningConfigurator, 30 seconds)
      finally {
        Configurator.closeRunningConfigurator = true
        service.shutdownNow()
        service.awaitTermination(60, TimeUnit.SECONDS)
      }
    }
  }
}
