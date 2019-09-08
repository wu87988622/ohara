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

package com.island.ohara.it.connector

import com.island.ohara.agent._
import com.island.ohara.agent.k8s.K8SClient
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.Configurator
import com.island.ohara.it.agent.ClusterNameHolder
import com.island.ohara.it.category.K8sConnectorGroup
import org.junit.Ignore
import org.junit.experimental.categories.Category

@Ignore("a nonexistent IT is better than chaotic IT. see https://github.com/oharastream/ohara/issues/2393")
@Category(Array(classOf[K8sConnectorGroup]))
class TestK8sPostgresqlJDBCSourceConnector extends BasicTestPostgresqlJDBCSourceConnector {

  private[this] val K8S_API_SERVER_URL_KEY: String = "ohara.it.k8s"
  private[this] val K8S_API_NODE_NAME_KEY: String = "ohara.it.k8s.nodename"

  private[this] val API_SERVER_URL: Option[String] = sys.env.get(K8S_API_SERVER_URL_KEY)
  private[this] val NODE_SERVER_NAME: Option[String] = sys.env.get(K8S_API_NODE_NAME_KEY)

  private[this] var _clusterCollie: ClusterCollie = _

  private[this] var _nameHolder: ClusterNameHolder = _

  override protected val nodeCache: Seq[Node] =
    if (API_SERVER_URL.isEmpty || NODE_SERVER_NAME.isEmpty) Seq.empty
    else
      NODE_SERVER_NAME.get
        .split(",")
        .map(node =>
          Node(
            hostname = node,
            port = Some(22),
            user = Some("fake"),
            password = Some("fake"),
            services = Seq.empty,
            lastModified = CommonUtils.current(),
            validationReport = None,
            tags = Map.empty
        ))

  override protected def checkClusterInfo(): Unit =
    if (nodeCache.isEmpty)
      skipTest(s"The k8s is skip test, Please setting $K8S_API_SERVER_URL_KEY and $K8S_API_NODE_NAME_KEY properties")
    else {
      _clusterCollie =
        ClusterCollie.builderOfK8s().nodeCollie(NodeCollie(nodeCache)).k8sClient(K8SClient(API_SERVER_URL.get)).build()

      _nameHolder = ClusterNameHolder(nodeCache, K8SClient(API_SERVER_URL.get))
    }

  override protected def clusterCollie: ClusterCollie = _clusterCollie

  override protected def nameHolder: ClusterNameHolder = _nameHolder

  override protected def createConfigurator(nodeCache: Seq[Node], hostname: String, port: Int): Configurator =
    Configurator.builder.hostname(hostname).port(port).k8sClient(K8SClient(API_SERVER_URL.get)).build()
}
