package com.island.ohara.agent
import com.island.ohara.agent.AgentJson.{ClusterDescription, ContainerDescription}
import com.island.ohara.agent.Collie.ClusterCreator
import com.island.ohara.common.annotations.Optional

/**
  * Collie is a cute dog helping us to "manage" a bunch of sheep.
  * @tparam T cluster description
  */
trait Collie[T <: ClusterDescription] extends Iterable[T] {

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
  def creator(): ClusterCreator[T]

  /**
    * get the containers information from a zookeeper cluster
    * @param clusterName cluster name
    * @return containers information
    */
  def containers(clusterName: String): Seq[ContainerDescription]

  /**
    * get the cluster information from a broker cluster
    * @param name cluster name
    * @return cluster information
    */
  def cluster(name: String): T = find(_.name == name).get

  /**
    * @param clusterName cluster name
    * @return true if the broker cluster exists
    */
  def exists(clusterName: String): Boolean = exists(_.name == clusterName)

  /**
    * @param clusterName cluster name
    * @return true if the broker cluster doesn't exist
    */
  def nonExists(clusterName: String): Boolean = !exists(clusterName)

  /**
    * add a node to a running broker cluster
    * @param clusterName cluster name
    * @param nodeName node name
    * @return updated broker cluster
    */
  def addNode(clusterName: String, nodeName: String): T

  /**
    * remove a node from a running broker cluster
    * @param clusterName cluster name
    * @param nodeName node name
    * @return updated broker cluster
    */
  def removeNode(clusterName: String, nodeName: String): T
}

object Collie {
  trait ClusterCreator[T <: ClusterDescription] {
    protected var imageName: String = _
    protected var clusterName: String = _
    @Optional("we have default image for each collie")
    def imageName(imageName: String): ClusterCreator.this.type = {
      this.imageName = imageName
      this
    }

    def clusterName(clusterName: String): ClusterCreator.this.type = {
      this.clusterName = clusterName
      this
    }

    def create(nodeName: String): T = create(Seq(nodeName))
    def create(nodeNames: Seq[String]): T
  }
}
