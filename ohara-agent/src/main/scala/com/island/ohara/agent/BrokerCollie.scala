package com.island.ohara.agent
import com.island.ohara.agent.AgentJson.BrokerCluster
import com.island.ohara.agent.BrokerCollie.ClusterCreator
import com.island.ohara.agent.DockerJson.ContainerDescription
import com.island.ohara.common.util.CloseOnce

trait BrokerCollie extends CloseOnce with Iterable[BrokerCluster] {

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
    * @return creator of broker cluster
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
    * @return true if the broker cluster exists
    */
  def exist(clusterName: String): Boolean = containers(clusterName).nonEmpty

  /**
    * @param clusterName cluster name
    * @return true if the broker cluster doesn't exist
    */
  def nonExist(clusterName: String): Boolean = !exist(clusterName)
}

object BrokerCollie {
  trait ClusterCreator {
    def imageName(imageName: String): ClusterCreator
    def clusterName(clusterName: String): ClusterCreator
    def zookeeperClusterName(name: String): ClusterCreator
    def clientPort(clientPort: Int): ClusterCreator
    def create(nodeName: String): BrokerCluster = create(Seq(nodeName))
    def create(nodeNames: Seq[String]): BrokerCluster
  }
  private[agent] val SCRIPT_NAME: String = "broker.sh"

  /**
    * ohara-it needs this property for testing.
    */
  private[ohara] val IMAGE_NAME_DEFAULT: String = "islandsystems/kafka:1.0.2"
  private[agent] val ID_KEY: String = "BROKER_ID"
  private[agent] val DATA_DIRECTORY_KEY: String = "BROKER_DATA_DIR"
  private[agent] val ZOOKEEPERS_KEY: String = "BROKER_ZOOKEEPERS"

  private[agent] val CLIENT_PORT_KEY: String = "BROKER_CLIENT_PORT"
  private[agent] val CLIENT_PORT_DEFAULT: Int = 9092
}
