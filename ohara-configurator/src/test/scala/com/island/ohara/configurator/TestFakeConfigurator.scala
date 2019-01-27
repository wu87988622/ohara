/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.configurator

import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
class TestFakeConfigurator extends SmallTest with Matchers {

  @Test
  def testFakeCluster(): Unit = {
    Seq(
      (1, 1),
      (1, 2),
      (2, 1),
      (99, 99)
    ).foreach {
      case (numberOfBrokers, numberOfWorkers) =>
        val configurator = Configurator.builder().fake(numberOfBrokers, numberOfWorkers).build()
        try {
          Await.result(configurator.clusterCollie.brokerCollie().clusters(), 10 seconds).size shouldBe numberOfBrokers
          Await.result(configurator.clusterCollie.workerCollie().clusters(), 10 seconds).size shouldBe numberOfWorkers
          Await
            .result(configurator.clusterCollie.clusters(), 10 seconds)
            .size shouldBe (numberOfBrokers + numberOfWorkers)
          val nodes = Await.result(configurator.store.values[Node], 10 seconds)
          nodes.isEmpty shouldBe false
          Await
            .result(configurator.clusterCollie.clusters(), 10 seconds)
            .flatMap(_._1.nodeNames)
            .foreach(name => nodes.exists(_.name == name) shouldBe true)
        } finally configurator.close()
    }
  }
}
