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

import com.island.ohara.agent.Collie.ClusterCreator
import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.CommonUtils

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Collie is a cute dog helping us to "manage" a bunch of sheep.
  * @tparam T cluster description
  */
trait Collie[T <: ClusterInfo, Creator <: ClusterCreator[T]] {

  /**
    * remove whole cluster by specified name. The process, mostly, has a graceful shutdown
    * which can guarantee the data consistency. However, the graceful downing whole cluster may take some time...
    *
    * @param clusterName cluster name
    * @param executionContext thread pool
    * @return true if it does remove a running cluster. Otherwise, false
    */
  def remove(clusterName: String)(implicit executionContext: ExecutionContext): Future[Boolean]

  /**
    * This method open a door to sub class to implement a force remove which kill whole cluster without graceful shutdown.
    * NOTED: The default implementation is reference to graceful remove.
    * @param clusterName cluster name
    * @param executionContext thread pool
    * @return true if it does remove a running cluster. Otherwise, false
    */
  def forceRemove(clusterName: String)(implicit executionContext: ExecutionContext): Future[Boolean] = remove(
    clusterName)

  /**
    * get logs from all containers.
    * NOTED: It is ok to get logs from a "dead" cluster.
    * @param clusterName cluster name
    * @return all log content from cluster. Each container has a log.
    */
  def logs(clusterName: String)(implicit executionContext: ExecutionContext): Future[Map[ContainerInfo, String]]

  /**
    * create a cluster creator
    * @return creator of cluster
    */
  def creator(): Creator

  /**
    * get the containers information from a1 cluster
    * @param clusterName cluster name
    * @return containers information
    */
  def containers(clusterName: String)(implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    cluster(clusterName).map(_._2)

  /**
    * fetch all clusters and belonging containers from cache.
    * Note: this function will only get running containers
    *
    * @param executionContext execution context
    * @return cluster and containers information
    */
  def clusters(implicit executionContext: ExecutionContext): Future[Map[T, Seq[ContainerInfo]]] =
    clusterWithAllContainers.map(
      entry =>
        // Currently, both k8s and pure docker have the same context of "RUNNING".
        // It is ok to filter container via RUNNING state.
        // Note: even if all containers are dead, the cluster information should be fetch also.
        entry.map { case (info, containers) => info -> containers.filter(_.state == ContainerState.RUNNING.name) })

  // Collie only care about active containers, but we need to trace the exited "orphan" containers for deleting them.
  // This method intend to fetch all containers of each cluster and we filter out needed containers in other methods.
  protected def clusterWithAllContainers(
    implicit executionContext: ExecutionContext): Future[Map[T, Seq[ContainerInfo]]]

  /**
    * get the cluster information from a cluster
    * @param name cluster name
    * @return cluster information
    */
  def cluster(name: String)(implicit executionContext: ExecutionContext): Future[(T, Seq[ContainerInfo])] =
    clusters.map(_.find(_._1.name == name).getOrElse(throw new NoSuchClusterException(s"$name doesn't exist")))

  /**
    * @param clusterName cluster name
    * @return true if the cluster exists
    */
  def exist(clusterName: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    clusters.map(_.exists(_._1.name == clusterName))

  /**
    * @param clusterName cluster name
    * @return true if the cluster doesn't exist
    */
  def nonExist(clusterName: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    exist(clusterName).map(!_)

  /**
    * add a node to a running cluster
    * NOTED: this is a async operation since graceful adding a node to a running service may be slow.
    * @param clusterName cluster name
    * @param nodeName node name
    * @return updated cluster
    */
  def addNode(clusterName: String, nodeName: String)(implicit executionContext: ExecutionContext): Future[T]

  /**
    * remove a node from a running cluster.
    * NOTED: this is a async operation since graceful downing a node from a running service may be slow.
    * @param clusterName cluster name
    * @param nodeName node name
    * @return true if it does remove a node from a running cluster. Otherwise, false
    */
  def removeNode(clusterName: String, nodeName: String)(implicit executionContext: ExecutionContext): Future[Boolean]
}

object Collie {
  trait ClusterCreator[T <: ClusterInfo] extends com.island.ohara.common.pattern.Creator[Future[T]] {
    protected var imageName: String = _
    protected var clusterName: String = _
    protected var nodeNames: Set[String] = Set.empty
    protected var executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    /**
      * set the creator according to another cluster info
      * @param clusterInfo another cluster info
      */
    final def copy(clusterInfo: T): ClusterCreator.this.type = {
      imageName(clusterInfo.imageName)
      clusterName(clusterInfo.name)
      nodeNames(clusterInfo.nodeNames)
      doCopy(clusterInfo)
      this
    }

    protected def doCopy(clusterInfo: T): Unit

    /**
      * set the image name used to create cluster's container.
      * Currently, there are three kind of services are supported by community. zk, bk and wk.
      * @param imageName image name
      * @return this creator
      */
    def imageName(imageName: String): ClusterCreator.this.type = {
      this.imageName = CommonUtils.requireNonEmpty(imageName)
      this
    }

    /**
      * set the cluster name. Noted: All collie impls are unsupported to use duplicate cluster name.
      * @param clusterName cluster name
      * @return this creator
      */
    def clusterName(clusterName: String): ClusterCreator.this.type = {
      this.clusterName = checkClusterName(CommonUtils.requireNonEmpty(clusterName))
      this
    }

    /**
      * Apart from the basic check, the sub class may have more specific restriction to name.
      * The sub class should throw exception if the input cluster name is illegal.
      * @param clusterName cluster name
      * @return origin cluster
      */
    protected def checkClusterName(clusterName: String): String = clusterName

    /**
      *  create a single-node cluster.
      *  NOTED: this is a async method since starting a cluster is always gradual.
      * @param nodeName node name
      * @return cluster description
      */
    def nodeName(nodeName: String): ClusterCreator.this.type = nodeNames(Set(nodeName))

    /**
      *  create a cluster.
      *  NOTED: this is a async method since starting a cluster is always gradual.
      * @param nodeNames nodes' name
      * @return cluster description
      */
    def nodeNames(nodeNames: Set[String]): ClusterCreator.this.type = {
      this.nodeNames = CommonUtils.requireNonEmpty(nodeNames.asJava).asScala.toSet
      this
    }

    /**
      * set the thread pool used to create cluster by async call
      * @param executionContext thread pool
      * @return this creator
      */
    @Optional("default pool is scala.concurrent.ExecutionContext.Implicits.global")
    def threadPool(executionContext: ExecutionContext): ClusterCreator.this.type = {
      this.executionContext = Objects.requireNonNull(executionContext)
      this
    }

    override def create(): Future[T]
  }
}
