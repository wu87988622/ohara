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

package com.island.ohara.agent.k8s

import com.island.ohara.agent.{NodeCollie, ZookeeperCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo

import scala.concurrent.{Await, ExecutionContext, Future}
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration

class TestK8SBasicCollieImpl extends SmallTest with Matchers {
  private[this] val TIMEOUT: FiniteDuration = 30 seconds

  @Test
  def testClusterName(): Unit = {
    val cluster1ContainerName = s"${PREFIX_KEY}${DIVIDER}cluster1${DIVIDER}zk${DIVIDER}00000"
    clusterName(cluster1ContainerName) shouldBe "cluster1"

    val cluster2 = s"${PREFIX_KEY}${DIVIDER}zk${DIVIDER}zk${DIVIDER}00000"
    clusterName(cluster2) shouldBe "zk"

    val cluster3 = s"${PREFIX_KEY}${DIVIDER}zkzk${DIVIDER}zk${DIVIDER}00000"
    clusterName(cluster3) shouldBe "zkzk"

    val cluster4 = s"${PREFIX_KEY}${DIVIDER}zk${DIVIDER}zk"
    clusterName(cluster4) shouldBe "zk"

    val cluster5 = s"${PREFIX_KEY}${DIVIDER}zk"
    an[IllegalArgumentException] should be thrownBy {
      clusterName(cluster5) shouldBe "zk"
    }
  }

  private[this] def clusterName(containerName: String): String = {
    val node1Name = "node1"
    val node1: Node = Node(node1Name, 22, "", "")
    val nodes: Seq[Node] = Seq(node1)
    val nodeCollie = NodeCollie(nodes)
    val basicCollieImpl =
      new K8SBasicCollieImpl[ZookeeperClusterInfo, ZookeeperCollie.ClusterCreator](nodeCollie, null) {

        override def creator(): Nothing =
          throw new UnsupportedOperationException("Test doesn't support creator function")

        override def filterContainerService(nodes: Seq[Node])(
          implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] = {
          Future {
            nodes.map(n => {
              ContainerInfo(n.name,
                            "0000",
                            "fakeimage",
                            "2019-05-21 00:00:00",
                            "running",
                            "unknow",
                            containerName,
                            "0",
                            Seq.empty,
                            Map.empty,
                            "host1")
            })
          }
        }

        override protected def toClusterDescription(clusterName: String, containers: Seq[ContainerInfo])(
          implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] = {
          Future {
            ZookeeperClusterInfo(clusterName, containers(0).imageName, 2181, 2182, 2183, Seq.empty)
          }
        }
      }

    val executionContext = ExecutionContext.Implicits.global
    val clusters: Map[ZookeeperClusterInfo, Seq[ContainerInfo]] =
      Await.result(basicCollieImpl.clusters(executionContext), TIMEOUT)
    clusters.head._1.name
  }
}
