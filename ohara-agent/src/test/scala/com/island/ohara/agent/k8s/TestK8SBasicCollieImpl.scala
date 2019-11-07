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

import com.island.ohara.agent.fake.FakeK8SClient
import com.island.ohara.agent.{Collie, DataCollie, ZookeeperCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterStatus
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{Await, ExecutionContext, Future}

class TestK8SBasicCollieImpl extends OharaTest with Matchers {
  private[this] val TIMEOUT: FiniteDuration = 30 seconds

  private[this] val tmpServiceName = "zk"

  @Test
  def testClusterName(): Unit = {
    val group = CommonUtils.randomString(10)
    val cluster1ContainerName = Collie.containerName(PREFIX_KEY, group, "cluster1", tmpServiceName)
    zookeeperClusterName(cluster1ContainerName) shouldBe "cluster1"

    val cluster2ContainerName = Collie.containerName(PREFIX_KEY, group, "zk", tmpServiceName)
    zookeeperClusterName(cluster2ContainerName) shouldBe "zk"

    val cluster3ContainerName = Collie.containerName(PREFIX_KEY, group, "zkzk", tmpServiceName)
    zookeeperClusterName(cluster3ContainerName) shouldBe "zkzk"

    val cluster4ContainerName = s"$PREFIX_KEY$DIVIDER$group${DIVIDER}zk$DIVIDER$tmpServiceName"
    zookeeperClusterName(cluster4ContainerName) shouldBe "zk"
  }

  private[this] def zookeeperClusterName(containerName: String): String = {
    val node1Name = "node1"
    val node1: Node = Node(
      hostname = node1Name,
      port = Some(22),
      user = Some("fake"),
      password = Some("fake"),
      services = Seq.empty,
      lastModified = CommonUtils.current(),
      validationReport = None,
      resources = Seq.empty,
      tags = Map.empty
    )
    val nodes: Seq[Node] = Seq(node1)
    val dataCollie = DataCollie(nodes)
    val k8sClient = new FakeK8SClient(true, None, containerName)

    val k8sBasicCollieImpl: K8SBasicCollieImpl[ZookeeperClusterStatus] =
      new K8SBasicCollieImpl[ZookeeperClusterStatus](dataCollie, k8sClient) {

        override def creator: ZookeeperCollie.ClusterCreator =
          throw new UnsupportedOperationException("Test doesn't support creator function")

        override def serviceName: String = tmpServiceName

        override protected[agent] def toStatus(key: ObjectKey, containers: Seq[ContainerInfo])(
          implicit executionContext: ExecutionContext): Future[ZookeeperClusterStatus] =
          Future.successful(
            new ZookeeperClusterStatus(
              group = key.group(),
              name = key.name(),
              aliveNodes = nodes.map(_.name).toSet,
              state = None,
              error = None
            ))
      }

    val containers = k8sBasicCollieImpl.clusterWithAllContainers()(Implicits.global)
    Await.result(containers, TIMEOUT).head._1.name
  }
}
