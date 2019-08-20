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
import com.island.ohara.agent.{ClusterCollie, NodeCollie}
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.it.agent.{BasicTests4ClusterCollie, ClusterNameHolder}
import org.junit.Before
class TestK8sClusterCollie extends BasicTests4ClusterCollie {
  private[this] val K8S_API_SERVER_URL_KEY: String = "ohara.it.k8s"
  private[this] val K8S_API_NODE_NAME_KEY: String = "ohara.it.k8s.nodename"

  private[this] val API_SERVER_URL: Option[String] = sys.env.get(K8S_API_SERVER_URL_KEY)
  private[this] val NODE_SERVER_NAME: Option[String] = sys.env.get(K8S_API_NODE_NAME_KEY)

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

  override protected def nameHolder: ClusterNameHolder = _nameHolder

  private[this] var _nameHolder: ClusterNameHolder = _

  override protected def clusterCollie: ClusterCollie = _clusterCollie
  private[this] var _clusterCollie: ClusterCollie = _

  @Before
  final def setup(): Unit = if (nodeCache.isEmpty)
    skipTest(s"The k8s is skip test, Please setting $K8S_API_SERVER_URL_KEY and $K8S_API_NODE_NAME_KEY properties")
  else {
    _clusterCollie =
      ClusterCollie.builderOfK8s().nodeCollie(NodeCollie(nodeCache)).k8sClient(K8SClient(API_SERVER_URL.get)).build()
    _nameHolder = ClusterNameHolder(nodeCache, K8SClient(API_SERVER_URL.get))
  }
}
