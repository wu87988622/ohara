package com.island.ohara.demo

import com.island.ohara.client.ConfiguratorClient
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.common.rule.LargeTest
import com.island.ohara.demo.Backend.ServicePorts
import org.junit.Test
import org.scalatest.Matchers

class TestBackendConfigurator extends LargeTest with Matchers {

  @Test
  def testConfigurator(): Unit = {
    Backend.run(
      ServicePorts.default,
      (configurator, _, _, _, _, _) => {
        val client = ConfiguratorClient("localhost", configurator.port)
        try {
          // it should pass
          client.cluster[ClusterInformation]
        } finally client.close()
      }
    )
  }
}
