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
import org.junit.Before

import scala.concurrent.Future

class TestK8SCollie extends BasicTestsOfCollie {
  private[this] val K8S_API_SERVER_URL_KEY: String = "ohara.it.k8s"
  private[this] val K8S_API_NODE_NAME_KEY: String = "ohara.it.k8s.nodename"

  private[this] val API_SERVER_URL: Option[String] = sys.env.get(K8S_API_SERVER_URL_KEY)
  private[this] val NODE_SERVER_NAME: Option[String] = sys.env.get(K8S_API_NODE_NAME_KEY)

  var nodeCache: Seq[Node] = Seq()
  implicit var k8sClient: K8SClient = _

  override protected def clusterCollie: ClusterCollie = ClusterCollie.k8s(
    new NodeCollie {
      override def nodes(): Future[Seq[Node]] = Future.successful(nodeCache)
      override def node(name: String): Future[Node] = Future.successful(
        nodeCache.find(_.name == name).getOrElse(throw new NoSuchElementException(s"expected:$name actual:$nodeCache")))
    },
    k8sClient
  )

  @Before
  final def setup(): Unit = {
    val message = s"The k8s is skip test, Please setting $K8S_API_SERVER_URL_KEY and $K8S_API_NODE_NAME_KEY properties"
    if (API_SERVER_URL.isEmpty || NODE_SERVER_NAME.isEmpty) {
      skipTest(message)
    }
    k8sClient = K8SClient(API_SERVER_URL.get)
    nodeCache = NODE_SERVER_NAME.get.split(",").map(node => NodeApi.node(node, 0, "", ""))
  }
}
