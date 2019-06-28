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

import com.island.ohara.client.configurator.v0.WorkerApi
import com.island.ohara.testing.WithBrokerWorker
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
class TestFetchConnectorsFromDynamicWorkerCluster extends WithBrokerWorker with Matchers {

  @Test
  def test(): Unit = {
    val configurator = Configurator.builder().fake(testUtil().brokersConnProps(), testUtil().workersConnProps()).build()

    try {
      val clusters =
        Await.result(WorkerApi.access.hostname(configurator.hostname).port(configurator.port).list(), 10 seconds)
      clusters.isEmpty shouldBe false

      clusters.foreach { cluster =>
        cluster.connectors.size should not be 0
        cluster.sinks.size should not be 0
        cluster.sources.size should not be 0
      }
    } finally configurator.close()
  }
}
