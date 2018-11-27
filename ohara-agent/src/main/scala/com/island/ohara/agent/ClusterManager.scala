package com.island.ohara.agent

import com.island.ohara.agent.DockerJson.ContainerDescription
import com.island.ohara.agent.KafkaJson.Node

import scala.reflect.ClassTag

trait ClusterManager {
  def containers(clusterName: String, service: String): Map[Node, Seq[ContainerDescription]]

  def removeCluster(name: String): Unit

  def addCluster(name: String, cluster: AnyRef): Unit

  def existCluster(name: String): Boolean

  def clusters[Cluster: ClassTag](): Seq[Cluster]

  def cluster[Cluster: ClassTag](name: String): Cluster

  def node(name: String): Node

  def hostname(service: String, index: Int): String

  def containerName(clusterName: String, service: String, index: Int): String
}
