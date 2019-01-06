package com.island.ohara.demo

import com.island.ohara.client.configurator.v0.InfoApi
import com.island.ohara.common.rule.LargeTest
import com.island.ohara.demo.Backend.ServicePorts
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
class TestBackendConfigurator extends LargeTest with Matchers {

  @Test
  def testConfigurator(): Unit = {
    Backend.run(
      ServicePorts.default,
      (configurator, _, _, _, _, _) => {
        // it should pass
        Await.result(InfoApi.access().hostname("localhost").port(configurator.port).get(), 10 seconds)
      }
    )
  }
}
