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

import com.island.ohara.agent.{ClusterCollie, NodeCollie}
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.it.EnvTestingUtils
import com.island.ohara.it.agent.{BasicTests4ClusterCollie, ClusterNameHolder}
import com.island.ohara.it.category.K8sCollieGroup
import org.junit.Before
import org.junit.experimental.categories.Category

@Category(Array(classOf[K8sCollieGroup]))
class TestK8sClusterCollie extends BasicTests4ClusterCollie {

  override protected val nodes: Seq[Node] = EnvTestingUtils.k8sNodes()

  override protected def nameHolder: ClusterNameHolder = _nameHolder

  private[this] var _nameHolder: ClusterNameHolder = _

  override protected def clusterCollie: ClusterCollie = _clusterCollie
  private[this] var _clusterCollie: ClusterCollie = _

  @Before
  final def setup(): Unit = {
    _clusterCollie =
      ClusterCollie.builderOfK8s().nodeCollie(NodeCollie(nodes)).k8sClient(EnvTestingUtils.k8sClient()).build()
    _nameHolder = ClusterNameHolder(nodes, EnvTestingUtils.k8sClient())
  }
}
