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

package com.island.ohara.it.agent
import com.island.ohara.agent.{ClusterCollie, K8SClient, NodeCollie}
import com.island.ohara.client.configurator.v0.NodeApi
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.util.Releasable
import com.island.ohara.configurator.Configurator
import com.typesafe.scalalogging.Logger
import org.junit.{After, Before}

import scala.concurrent.ExecutionContext.Implicits.global

class TestK8SClusterCollieByConfigurator extends BasicTests4ClusterCollieByConfigurator {
  private[this] val log = Logger(classOf[TestK8SClusterCollieByConfigurator])
  private[this] val K8S_API_SERVER_URL_KEY: String = "ohara.it.k8s"
  private[this] val K8S_API_NODE_NAME_KEY: String = "ohara.it.k8s.nodename"

  private[this] val API_SERVER_URL: Option[String] = sys.env.get(K8S_API_SERVER_URL_KEY)
  private[this] val NODE_SERVER_NAME: Option[String] = sys.env.get(K8S_API_NODE_NAME_KEY)

  override protected val nodeCache: Seq[Node] =
    if (API_SERVER_URL.isEmpty || NODE_SERVER_NAME.isEmpty) {
      //After getting K8S client URL, set the k8sCollie to Configurator object.
      //if API_SERVER_URL variable is null will skip the test.
      skipTest(s"You must assign nodes for collie tests")
      Seq.empty
    } else NODE_SERVER_NAME.get.split(",").map(node => Node(node, 0, "", ""))

  private[this] val k8sClient = K8SClient(API_SERVER_URL.get)
  val k8sCollie: ClusterCollie = ClusterCollie.k8s(
    NodeCollie(nodeCache),
    // It is ok to pass null since we will skip test if no k8s env exists
    if (API_SERVER_URL.isEmpty) null else k8sClient
  )

  val configurator: Configurator = Configurator.builder().clusterCollie(k8sCollie).build()

  private[this] val nameHolder = new ClusterNameHolder(nodeCache) {
    override def close(): Unit = {
      nodeCache.foreach { node =>
        try k8sClient.containers
          .filter(container => usedClusterNames.exists(clusterName => container.name.contains(clusterName)))
          .foreach { container =>
            try {
              k8sClient.remove(container.name)
              log.info(s"succeed to remove container ${container.name}")
            } catch {
              case e: Throwable =>
                log.error(s"failed to remove container ${container.name}", e)
            }
          } finally k8sClient.close()
      }
    }
  }

  @Before
  final def setup(): Unit = {
    if (nodeCache.isEmpty) skipTest(s"You must assign nodes for collie tests")
    else {
      val nodeApi = NodeApi.access().hostname(configurator.hostname).port(configurator.port)
      nodeCache.foreach { node =>
        result(
          nodeApi.add(
            NodeApi.NodeCreationRequest(
              name = Some(node.name),
              port = node.port,
              user = node.user,
              password = node.password
            )))
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
