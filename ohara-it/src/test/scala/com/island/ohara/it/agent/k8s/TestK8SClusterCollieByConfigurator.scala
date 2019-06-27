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

package com.island.ohara.it.agent.k8s

import com.island.ohara.agent.k8s.K8SClient
import com.island.ohara.client.configurator.v0.NodeApi
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import com.island.ohara.it.agent.{BasicTests4ClusterCollieByConfigurator, ClusterNameHolder}
import com.typesafe.scalalogging.Logger
import org.junit.{After, Before}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{FiniteDuration, _}

class TestK8SClusterCollieByConfigurator extends BasicTests4ClusterCollieByConfigurator {
  private[this] val log = Logger(classOf[TestK8SClusterCollieByConfigurator])
  private[this] val K8S_API_SERVER_URL_KEY: String = "ohara.it.k8s"
  private[this] val K8S_API_NODE_NAME_KEY: String = "ohara.it.k8s.nodename"

  private[this] val API_SERVER_URL: Option[String] = sys.env.get(K8S_API_SERVER_URL_KEY)
  private[this] val NODE_SERVER_NAME: Option[String] = sys.env.get(K8S_API_NODE_NAME_KEY)

  private[this] val TIMEOUT: FiniteDuration = 30 seconds

  override protected val nodeCache: Seq[Node] =
    if (API_SERVER_URL.isEmpty || NODE_SERVER_NAME.isEmpty) Seq.empty
    else
      NODE_SERVER_NAME.get
        .split(",")
        .map(
          node =>
            Node(name = node,
                 port = 22,
                 user = "fake",
                 password = "fake",
                 services = Seq.empty,
                 lastModified = CommonUtils.current()))

  override def configurator: Configurator = _configurator
  private[this] var _configurator: Configurator = _
  private[this] var nameHolder: ClusterNameHolder = _

  @Before
  final def setup(): Unit = {
    if (nodeCache.isEmpty) skipTest(s"You must assign nodes for collie tests")
    else {
      _configurator = Configurator.builder().k8sClient(K8SClient(API_SERVER_URL.get)).build()
      nameHolder = new ClusterNameHolder(nodeCache) {
        override def close(): Unit = {
          val k8sClient = K8SClient(API_SERVER_URL.get)
          try {
            nodeCache.foreach { node =>
              Await
                .result(k8sClient.containers, TIMEOUT)
                .filter(container => usedClusterNames.exists(clusterName => container.name.contains(clusterName)))
                .foreach { container =>
                  try {
                    k8sClient.remove(container.name)
                    log.info(s"succeed to remove container ${container.name}")
                  } catch {
                    case e: Throwable =>
                      log.error(s"failed to remove container ${container.name}", e)
                  }
                }
            }
          } finally k8sClient.close()
        }
      }
      val nodeApi = NodeApi.access.hostname(configurator.hostname).port(configurator.port)
      nodeCache.foreach { node =>
        result(nodeApi.request.name(node.name).port(node.port).user(node.user).password(node.password).create())
      }
      val nodes = result(nodeApi.list)
      nodes.size shouldBe nodeCache.size
      nodeCache.foreach(node => nodes.exists(_.name == node.name) shouldBe true)
    }
  }

  @After
  def cleanAllContainers(): Unit = if (cleanup) Releasable.close(nameHolder)

  override protected def generateClusterName(): String = nameHolder.generateClusterName()
}
