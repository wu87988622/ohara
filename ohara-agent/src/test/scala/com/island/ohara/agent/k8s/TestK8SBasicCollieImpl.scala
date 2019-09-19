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
import com.island.ohara.agent.{Collie, NodeCollie, ZookeeperCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.ZookeeperApi
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
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
    val cluster1ContainerName = Collie.format(PREFIX_KEY, group, "cluster1", tmpServiceName)
    zookeeperClusterName(cluster1ContainerName) shouldBe "cluster1"

    val cluster2ContainerName = Collie.format(PREFIX_KEY, group, "zk", tmpServiceName)
    zookeeperClusterName(cluster2ContainerName) shouldBe "zk"

    val cluster3ContainerName = Collie.format(PREFIX_KEY, group, "zkzk", tmpServiceName)
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
      tags = Map.empty
    )
    val nodes: Seq[Node] = Seq(node1)
    val nodeCollie = NodeCollie(nodes)
    val k8sClient = new FakeK8SClient(true, None, containerName)

    val k8sBasicCollieImpl: K8SBasicCollieImpl[ZookeeperClusterInfo] =
      new K8SBasicCollieImpl[ZookeeperClusterInfo](nodeCollie, k8sClient) {
        override protected def toClusterDescription(objectKey: ObjectKey, containers: Seq[ContainerInfo])(
          implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] =
          Future.successful(
            ZookeeperClusterInfo(
              settings = ZookeeperApi.access.request
                .name(objectKey.name())
                .group(objectKey.group())
                .imageName(containers.head.imageName)
                .nodeNames(nodes.map(_.name).toSet)
                .creation
                .settings,
              deadNodes = Set.empty,
              state = None,
              error = None,
              lastModified = 0
            ))

        override def creator: ZookeeperCollie.ClusterCreator =
          throw new UnsupportedOperationException("Test doesn't support creator function")

        override def serviceName: String = tmpServiceName
      }

    val containers = k8sBasicCollieImpl.clusterWithAllContainers()(Implicits.global)
    Await.result(containers, TIMEOUT).head._1.name
  }
}
