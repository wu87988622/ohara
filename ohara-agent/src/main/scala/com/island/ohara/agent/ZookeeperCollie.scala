package com.island.ohara.agent
import com.island.ohara.agent.AgentJson.ZookeeperCluster
import com.island.ohara.agent.DockerJson.ContainerDescription
import com.island.ohara.agent.ZookeeperCollie.ClusterCreator
import com.island.ohara.common.util.CloseOnce

/**
  * a interface of controlling zookeeper cluster.
  * It isolates the implementation of container manager from Configurator.
  */
trait ZookeeperCollie extends CloseOnce with Iterable[ZookeeperCluster] {

  /**
    * remove whole cluster by specified name
    * @param clusterName cluster name
    */
  def remove(clusterName: String): Unit

  /**
    * get logs from all containers
    * @param clusterName cluster name
    * @return all log content from cluster. Each container has a log.
    */
  def logs(clusterName: String): Map[ContainerDescription, String]

  /**
    * create a cluster creator
    * @return creator of zookeeper cluster
    */
  def creator(): ClusterCreator

  /**
    * get the containers information from a zookeeper cluster
    * @param clusterName cluster name
    * @return containers information
    */
  def containers(clusterName: String): Seq[ContainerDescription]

  /**
    * @param clusterName cluster name
    * @return true if the zk cluster exists
    */
  def exist(clusterName: String): Boolean = containers(clusterName).nonEmpty

  /**
    * @param clusterName cluster name
    * @return true if the zk cluster doesn't exist
    */
  def nonExist(clusterName: String): Boolean = !exist(clusterName)
}

object ZookeeperCollie {

  trait ClusterCreator {
    def imageName(imageName: String): ClusterCreator
    def clusterName(name: String): ClusterCreator
    def clientPort(clientPort: Int): ClusterCreator
    def peerPort(peerPort: Int): ClusterCreator
    def electionPort(electionPort: Int): ClusterCreator
    def create(nodeName: String): ZookeeperCluster = create(Seq(nodeName))
    def create(nodeNames: Seq[String]): ZookeeperCluster
  }

  /**
    * This property is useless now since our zookeepr image has "fixed" at specified script.
    * see docker/zookeeper.release.dockerfile for more details
    */
  private[agent] val SCRIPT_NAME: String = "zk.sh"

  /**
    * ohara-it needs this property for testing.
    */
  private[ohara] val IMAGE_NAME_DEFAULT: String = "islandsystems/zookeeper:3.4.13"

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
