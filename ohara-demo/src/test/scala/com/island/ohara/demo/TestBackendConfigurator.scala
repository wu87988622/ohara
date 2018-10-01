package com.island.ohara.demo

import com.island.ohara.client.ConfiguratorClient
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.rule.LargeTest
import org.junit.Test
import org.scalatest.Matchers

class TestBackendConfigurator extends LargeTest with Matchers {

  @Test
  def testConfigurator(): Unit = {
    Backend.run(
      0,
      (configurator, db) => {
        val client = ConfiguratorClient("localhost", configurator.port)
        try {
          // it should pass
          client.cluster[ClusterInformation]
        } finally client.close()
      }
    )
  }
}
