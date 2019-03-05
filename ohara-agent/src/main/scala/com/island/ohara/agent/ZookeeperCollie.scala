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
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.CommonUtil

import scala.concurrent.Future

/**
  * An interface of controlling zookeeper cluster.
  * It isolates the implementation of container manager from Configurator.
  */
trait ZookeeperCollie extends Collie[ZookeeperClusterInfo, ZookeeperCollie.ClusterCreator]

object ZookeeperCollie {
  trait ClusterCreator extends Collie.ClusterCreator[ZookeeperClusterInfo] {
    private[this] var clientPort: Int = CommonUtil.availablePort()
    private[this] var peerPort: Int = CommonUtil.availablePort()
    private[this] var electionPort: Int = CommonUtil.availablePort()

    @Optional("default is random port")
    def clientPort(clientPort: Int): ClusterCreator = {
      this.clientPort = CommonUtil.requirePositiveInt(clientPort)
      this
    }

    @Optional("default is random port")
    def peerPort(peerPort: Int): ClusterCreator = {
      this.peerPort = CommonUtil.requirePositiveInt(peerPort)
      this
    }

    @Optional("default is random port")
    def electionPort(electionPort: Int): ClusterCreator = {
      this.electionPort = CommonUtil.requirePositiveInt(electionPort)
      this
    }

    override def create(): Future[ZookeeperClusterInfo] = doCreate(
      clusterName = CommonUtil.requireNonEmpty(clusterName),
      imageName = CommonUtil.requireNonEmpty(imageName),
      clientPort = CommonUtil.requirePositiveInt(clientPort),
      peerPort = CommonUtil.requirePositiveInt(peerPort),
      electionPort = CommonUtil.requirePositiveInt(electionPort),
      nodeNames = requireNonEmpty(nodeNames)
    )

    protected def doCreate(clusterName: String,
                           imageName: String,
                           clientPort: Int,
                           peerPort: Int,
                           electionPort: Int,
                           nodeNames: Seq[String]): Future[ZookeeperClusterInfo]
  }

  private[agent] val CLIENT_PORT_KEY: String = "ZK_CLIENT_PORT"

  private[agent] val PEER_PORT_KEY: String = "ZK_PEER_PORT"

  private[agent] val ELECTION_PORT_KEY: String = "ZK_ELECTION_PORT"

  private[agent] val DATA_DIRECTORY_KEY: String = "ZK_DATA_DIR"
  private[agent] val SERVERS_KEY: String = "ZK_SERVERS"
  private[agent] val ID_KEY: String = "ZK_ID"
}
