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

import com.island.ohara.agent.ClusterCollie
import com.island.ohara.agent.k8s.K8SClient
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Await

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
class TestConfiguratorBuilder extends SmallTest with Matchers {
  @Test
  def nullHomeFolder(): Unit = an[NullPointerException] should be thrownBy Configurator.builder.homeFolder(null)

  @Test
  def emptyHomeFolder(): Unit = an[IllegalArgumentException] should be thrownBy Configurator.builder.homeFolder("")

  @Test
  def fileToHomeFolder(): Unit = an[IllegalArgumentException] should be thrownBy Configurator.builder.homeFolder(
    CommonUtils.createTempJar(methodName()).getCanonicalPath)

  @Test
  def autoMkdirForHomeFolder(): Unit = {
    val folder = CommonUtils.createTempFolder(methodName())
    folder.delete() shouldBe true
    folder.exists() shouldBe false
    Configurator.builder.homeFolder(folder.getCanonicalPath)
    folder.exists() shouldBe true
  }

  @Test
  def duplicatePort(): Unit = an[IllegalArgumentException] should be thrownBy Configurator.builder.port(10).port(20)

  @Test
  def testFakeCluster(): Unit = {
    Seq(
      (1, 1),
      (1, 2),
      (2, 1),
      (99, 99)
    ).foreach {
      case (numberOfBrokers, numberOfWorkers) =>
        val configurator = Configurator.builder.fake(numberOfBrokers, numberOfWorkers).build()
        try {
          Await.result(configurator.clusterCollie.brokerCollie.clusters(), 20 seconds).size shouldBe numberOfBrokers
          Await.result(configurator.clusterCollie.workerCollie.clusters(), 20 seconds).size shouldBe numberOfWorkers
          Await
            .result(configurator.clusterCollie.clusters(), 20 seconds)
            // one broker generates one zk cluster
            .size shouldBe (numberOfBrokers + numberOfBrokers + numberOfWorkers)
          val nodes = Await.result(configurator.store.values[Node](), 20 seconds)
          nodes.isEmpty shouldBe false
          Await
            .result(configurator.clusterCollie.clusters(), 20 seconds)
            .flatMap(_._1.nodeNames)
            .foreach(name => nodes.exists(_.name == name) shouldBe true)
        } finally configurator.close()
    }
  }

  @Test
  def createWorkerClusterWithoutBrokerCluster(): Unit = {
    an[IllegalArgumentException] should be thrownBy Configurator.builder.fake(0, 1)
  }

  @Test
  def createFakeConfiguratorWithoutClusters(): Unit = {
    val configurator = Configurator.builder.fake(0, 0).build()
    try Await.result(configurator.clusterCollie.clusters(), 20 seconds).size shouldBe 0
    finally configurator.close()
  }

  @Test
  def reassignClusterCollieAfterFake(): Unit =
    an[IllegalArgumentException] should be thrownBy Configurator.builder
    // in fake mode, a fake collie will be created
      .fake(1, 1)
      .clusterCollie(MockitoSugar.mock[ClusterCollie])
      .build()

  @Test
  def reassignK8sClient(): Unit = an[IllegalArgumentException] should be thrownBy Configurator.builder
    .k8sClient(MockitoSugar.mock[K8SClient])
    .k8sClient(MockitoSugar.mock[K8SClient])
    .build()

  @Test
  def reassignClusterCollie(): Unit = an[IllegalArgumentException] should be thrownBy Configurator.builder
    .clusterCollie(MockitoSugar.mock[ClusterCollie])
    .clusterCollie(MockitoSugar.mock[ClusterCollie])
    .build()

  @Test
  def reassignHostname(): Unit = an[IllegalArgumentException] should be thrownBy Configurator.builder
    .hostname(CommonUtils.hostname())
    .hostname(CommonUtils.hostname())
    .build()

  @Test
  def reassignPort(): Unit = an[IllegalArgumentException] should be thrownBy Configurator.builder
    .port(CommonUtils.availablePort())
    .port(CommonUtils.availablePort())
    .build()

  @Test
  def reassignHomeFolder(): Unit = an[IllegalArgumentException] should be thrownBy Configurator.builder
    .homeFolder(CommonUtils.createTempFolder(methodName()).getCanonicalPath)
    .homeFolder(CommonUtils.createTempFolder(methodName()).getCanonicalPath)
    .build()

  @Test
  def reassignHomeFolderAfterFake(): Unit = an[IllegalArgumentException] should be thrownBy Configurator.builder
  // in fake mode, we have created a store
    .fake(1, 1)
    // you can't change the folder of store now
    .homeFolder(CommonUtils.createTempFolder(methodName()).getCanonicalPath)
    .build()
}
