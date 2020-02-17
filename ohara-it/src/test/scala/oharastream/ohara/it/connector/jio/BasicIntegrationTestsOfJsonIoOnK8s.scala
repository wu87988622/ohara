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

package oharastream.ohara.it.connector.jio

import oharastream.ohara.client.configurator.v0.NodeApi
import oharastream.ohara.client.configurator.v0.NodeApi.Node
import oharastream.ohara.client.kafka.ConnectorAdmin
import oharastream.ohara.configurator.Configurator
import oharastream.ohara.it.{EnvTestingUtils, ServiceKeyHolder}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * initialize the configurator on k8s mode.
  */
abstract class BasicIntegrationTestsOfJsonIoOnK8s extends BasicIntegrationTestsOfJsonIo {
  private[this] val nodes: Seq[Node] = EnvTestingUtils.k8sNodes()
  override protected val configurator: Configurator = {
    val configurator = Configurator.builder.k8sClient(EnvTestingUtils.k8sClient()).build()
    val nodeApi      = NodeApi.access.hostname(configurator.hostname).port(configurator.port)
    nodes.foreach(node => result(nodeApi.request.node(node).create()))
    configurator
  }
  override protected val nameHolder: ServiceKeyHolder = ServiceKeyHolder(EnvTestingUtils.k8sClient())

  /**
    * we initialize the clusters in setup phase so there is nothing in construction .
    */
  protected def connectorAdmin: ConnectorAdmin = _connectorAdmin

  /**
    * we initialize the clusters in setup phase so there is nothing in construction .
    */
  protected def brokersConnProps: String = _brokersConnProps
}
