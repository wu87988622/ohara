package com.island.ohara.agent
import com.island.ohara.agent.AgentJson.WorkerCluster
import com.island.ohara.agent.DockerJson.ContainerDescription
import com.island.ohara.agent.WorkerCollie.ClusterCreator
import com.island.ohara.common.util.CloseOnce

trait WorkerCollie extends CloseOnce with Iterable[WorkerCluster] {

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
    * @return creator of worker cluster
    */
  def creator(): ClusterCreator

  /**
    * get the containers information from a worker cluster
    * @param clusterName cluster name
    * @return containers information
    */
  def containers(clusterName: String): Seq[ContainerDescription]

  /**
    * @param clusterName cluster name
    * @return true if the worker cluster exists
    */
  def exist(clusterName: String): Boolean = containers(clusterName).nonEmpty

  /**
    * @param clusterName cluster name
    * @return true if the worker cluster doesn't exist
    */
  def nonExist(clusterName: String): Boolean = !exist(clusterName)
}

object WorkerCollie {
  trait ClusterCreator {
    def imageName(imageName: String): ClusterCreator
    def clusterName(clusterName: String): ClusterCreator
    def brokerClusterName(name: String): ClusterCreator
    def clientPort(clientPort: Int): ClusterCreator
    def create(nodeName: String): WorkerCluster = create(Seq(nodeName))
    def create(nodeNames: Seq[String]): WorkerCluster
  }
  private[agent] val SCRIPT_NAME: String = "worker.sh"

  /**
    * ohara-it needs this property for testing.
    */
  private[ohara] val IMAGE_NAME_DEFAULT: String = "islandsystems/kafka:1.0.2"
  private[agent] val GROUP_ID_KEY: String = "WORKER_GROUP"
  private[agent] val OFFSET_TOPIC_KEY: String = "WORKER_OFFSET_TOPIC"
  private[agent] val CONFIG_TOPIC_KEY: String = "WORKER_CONFIG_TOPIC"
  private[agent] val STATUS_TOPIC_KEY: String = "WORKER_STATUS_TOPIC"
  private[agent] val BROKERS_KEY: String = "WORKER_BROKERS"
  private[agent] val CLIENT_PORT_KEY: String = "WORKER_CLIENT_PORT"
  private[agent] val CLIENT_PORT_DEFAULT: Int = 8083

}
