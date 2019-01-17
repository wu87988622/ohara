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

package com.island.ohara.agent
import java.util.Objects

import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.{CommonUtil, VersionUtil}

import scala.concurrent.Future

trait BrokerCollie extends Collie[BrokerClusterInfo] {
  def creator(): BrokerCollie.ClusterCreator
}

object BrokerCollie {
  trait ClusterCreator extends Collie.ClusterCreator[BrokerClusterInfo] {
    private[this] var clientPort: Int = BrokerCollie.CLIENT_PORT_DEFAULT
    private[this] var zookeeperClusterName: String = _
    private[this] var exporterPort: Int = BrokerCollie.EXPORTER_PORT_DEFAULT

    def zookeeperClusterName(name: String): ClusterCreator = {
      this.zookeeperClusterName = name
      this
    }

    @Optional("default port is 9092")
    def clientPort(clientPort: Option[Int]): ClusterCreator = {
      clientPort.foreach(this.clientPort = _)
      this
    }

    def clientPort(port: Int): ClusterCreator = clientPort(Some(port))

    @Optional("default port is 7071")
    def exporterPort(exporterPort: Int): ClusterCreator = {
      this.exporterPort = exporterPort
      this
    }

    override def create(): Future[BrokerClusterInfo] = doCreate(
      clusterName = Objects.requireNonNull(clusterName),
      imageName = Option(imageName).getOrElse(BrokerCollie.IMAGE_NAME_DEFAULT),
      zookeeperClusterName = Objects.requireNonNull(zookeeperClusterName),
      clientPort = CommonUtil.requirePositiveInt(clientPort, () => "clientPort must be positive"),
      exporterPort = CommonUtil.requirePositiveInt(exporterPort, () => "exporterPort must be positive"),
      nodeNames =
        if (nodeNames == null || nodeNames.isEmpty) throw new IllegalArgumentException("nodes can't be empty")
        else nodeNames
    )

    protected def doCreate(clusterName: String,
                           imageName: String,
                           zookeeperClusterName: String,
                           clientPort: Int,
                           exporterPort: Int,
                           nodeNames: Seq[String]): Future[BrokerClusterInfo]
  }

  /**
    * ohara-it needs this property for testing.
    */
  private[ohara] val IMAGE_NAME_DEFAULT: String = s"oharastream/broker:${VersionUtil.VERSION}"
  private[agent] val ID_KEY: String = "BROKER_ID"
  private[agent] val DATA_DIRECTORY_KEY: String = "BROKER_DATA_DIR"
  private[agent] val ZOOKEEPERS_KEY: String = "BROKER_ZOOKEEPERS"

  private[agent] val CLIENT_PORT_KEY: String = "BROKER_CLIENT_PORT"
  private[agent] val CLIENT_PORT_DEFAULT: Int = 9092

  private[agent] val ADVERTISED_HOSTNAME_KEY: String = "BROKER_ADVERTISED_HOSTNAME"
  private[agent] val ADVERTISED_CLIENT_PORT_KEY: String = "BROKER_ADVERTISED_CLIENT_PORT"

  private[agent] val EXPORTER_PORT_KEY: String = "PROMETHEUS_EXPORTER_PORT"
  private[agent] val EXPORTER_PORT_DEFAULT: Int = 7071
}
