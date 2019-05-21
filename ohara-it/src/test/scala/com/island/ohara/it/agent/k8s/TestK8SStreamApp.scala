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
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.configurator.Configurator
import com.island.ohara.it.agent.{BasicTests4StreamApp, ClusterNameHolder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestK8SStreamApp extends BasicTests4StreamApp {

  private[this] val API_SERVER_URL: Option[String] = sys.env.get("ohara.it.k8s")
  private[this] val NODE_SERVER_NAME: Option[String] = sys.env.get("ohara.it.k8s.nodename")

  override protected def createNodes(): Seq[Node] = if (API_SERVER_URL.isEmpty || NODE_SERVER_NAME.isEmpty) Seq.empty
  else NODE_SERVER_NAME.get.split(",").map(node => Node(node, 0, "", ""))
  override protected def createNameHolder(nodeCache: Seq[Node]): ClusterNameHolder = new ClusterNameHolder(nodeCache) {
    override def close(): Unit = {
      val k8sClient = K8SClient(API_SERVER_URL.get)
      Await.result(
        k8sClient.containers
          .map {
            _.filter(container => {
              usedClusterNames.exists(clusterName => {
                container.name.contains(clusterName)
              })
            })
          }
          .flatMap(Future.traverse(_) { container =>
            k8sClient.remove(container.name)
          }),
        30 seconds
      )
    }
  }
  override protected def createConfigurator(nodeCache: Seq[Node]): Configurator =
    Configurator.builder().k8sClient(K8SClient(API_SERVER_URL.get)).build()
}
