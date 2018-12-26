package com.island.ohara.agent
import com.island.ohara.client.ConfiguratorJson.ZookeeperClusterDescription
import com.island.ohara.common.util.{Releasable, VersionUtil}

/**
  * a interface of controlling zookeeper cluster.
  * It isolates the implementation of container manager from Configurator.
  */
trait ZookeeperCollie extends Releasable with Collie[ZookeeperClusterDescription] {
  override def creator(): ZookeeperCollie.ClusterCreator
}

object ZookeeperCollie {
  trait ClusterCreator extends Collie.ClusterCreator[ZookeeperClusterDescription] {
    def clientPort(clientPort: Int): ClusterCreator
    def peerPort(peerPort: Int): ClusterCreator
    def electionPort(electionPort: Int): ClusterCreator
    def create(nodeNames: Seq[String]): ZookeeperClusterDescription
  }

  /**
    * This property is useless now since our zookeepr image has "fixed" at specified script.
    * see docker/zookeeper.release.dockerfile for more details
    */
  private[agent] val SCRIPT_NAME: String = "zk.sh"

  /**
    * ohara-it needs this property for testing.
    */
  private[ohara] val IMAGE_NAME_DEFAULT: String = s"islandsystems/zookeeper:${VersionUtil.VERSION}"

  private[agent] val CLIENT_PORT_KEY: String = "ZK_CLIENT_PORT"
  private[agent] val CLIENT_PORT_DEFAULT: Int = 2181

  private[agent] val PEER_PORT_KEY: String = "ZK_PEER_PORT"
  private[agent] val PEER_PORT_DEFAULT: Int = 2888

  private[agent] val ELECTION_PORT_KEY: String = "ZK_ELECTION_PORT"
  private[agent] val ELECTION_PORT_DEFAULT: Int = 3888

  private[agent] val DATA_DIRECTORY_KEY: String = "ZK_DATA_DIR"
  private[agent] val SERVERS_KEY: String = "ZK_SERVERS"
  private[agent] val ID_KEY: String = "ZK_ID"
}
