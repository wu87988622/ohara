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
import com.island.ohara.client.configurator.v0.{ClusterInfo, ClusterRequest}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils

import scala.concurrent.{ExecutionContext, Future}

/**
  * Collie is a cute dog helping us to "manage" a bunch of sheep.
  * @tparam T cluster description
  */
trait Collie[T <: ClusterInfo] {

  /**
    * remove whole cluster by specified key. The process, mostly, has a graceful shutdown
    * which can guarantee the data consistency. However, the graceful downing whole cluster may take some time...
    *
    * @param key cluster key
    * @param executionContext thread pool
    * @return true if it does remove a running cluster. Otherwise, false
    */
  final def remove(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Boolean] =
    clusterWithAllContainers().flatMap(_.find(_._1.key == key).fold(Future.successful(false)) {
      case (cluster, containerInfos) => doRemove(cluster, containerInfos)
    })

  /**
    * remove whole cluster gracefully.
    * @param clusterInfo cluster info
    * @param containerInfos containers info
    * @param executionContext thread pool
    * @return true if success. otherwise false
    */
  protected def doRemove(clusterInfo: T, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean]

  /**
    * This method open a door to sub class to implement a force remove which kill whole cluster without graceful shutdown.
    * NOTED: The default implementation is reference to graceful remove.
    * @param key cluster key
    * @param executionContext thread pool
    * @return true if it does remove a running cluster. Otherwise, false
    */
  final def forceRemove(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Boolean] =
    clusterWithAllContainers().flatMap(_.find(_._1.key == key).fold(Future.successful(false)) {
      case (cluster, containerInfos) => doForceRemove(cluster, containerInfos)
    })

  /**
    * remove whole cluster forcely. the impl, by default, is similar to doRemove().
    * @param clusterInfo cluster info
    * @param containerInfos containers info
    * @param executionContext thread pool
    * @return true if success. otherwise false
    */
  protected def doForceRemove(clusterInfo: T, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean] = doRemove(clusterInfo, containerInfos)

  /**
    * get logs from all containers.
    * NOTED: It is ok to get logs from a "dead" cluster.
    * @param key cluster key
    * @return all log content from cluster. Each container has a log.
    */
  def logs(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Map[ContainerInfo, String]]

  /**
    * create a cluster creator. The subclass should have following rules.
    * 1) create an new cluster if there is no specific cluster
    * 2) add new nodes to a running cluster if there is a running cluster and the new nodes is different from running nodes
    * for example, running nodes are a0 and a1, and the new settings carries a1 and a2. The sub class should assume user
    * want to add an new node "a2" to the running cluster.
    * 3) otherwise, does nothing.
    *
    * However, it probably throw exception in facing 2) condition if the cluster does NOT support to add node at runtime
    * (for example, zookeeper).
    * @return creator of cluster
    */
  def creator: ClusterCreator

  /**
    * get the containers information from cluster
    * @param key cluster key
    * @return containers information
    */
  def containers(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    cluster(key).map(_._2)

  /**
    * fetch all clusters and belonging containers from cache.
    * Note: this function will only get running containers
    *
    * @param executionContext execution context
    * @return cluster and containers information
    */
  def clusters()(implicit executionContext: ExecutionContext): Future[Map[T, Seq[ContainerInfo]]] =
    clusterWithAllContainers().map(
      entry =>
        // Currently, both k8s and pure docker have the same context of "RUNNING".
        // It is ok to filter container via RUNNING state.
        // Note: even if all containers are dead, the cluster information should be fetch also.
        entry.map { case (info, containers) => info -> containers.filter(_.state == ContainerState.RUNNING.name) })

  // Collie only care about active containers, but we need to trace the exited "orphan" containers for deleting them.
  // This method intend to fetch all containers of each cluster and we filter out needed containers in other methods.
  protected def clusterWithAllContainers()(
    implicit executionContext: ExecutionContext): Future[Map[T, Seq[ContainerInfo]]]

  /**
    * get the cluster information from a cluster
    * @param key cluster key
    * @return cluster information
    */
  def cluster(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[(T, Seq[ContainerInfo])] =
    clusters().map(
      _.find(_._1.key == key)
        .getOrElse(throw new NoSuchClusterException(s"cluster with objectKey [$key] is not running")))

  /**
    * This is a helper method for those caller only knows cluster name.
    *
    * @param clusterName the cluster name in current type T
    * @param executionContext execution context
    * @return cluster information
    */
  def cluster(clusterName: String)(implicit executionContext: ExecutionContext): Future[(T, Seq[ContainerInfo])] =
    clusters().map(
      clusters =>
        // We need to filter by current type since there may have same name in different cluster type
        clusters
          .filter(_._1.isInstanceOf[T])
          .find(_._1.name == clusterName)
          .getOrElse(throw new NoSuchClusterException(s"cluster with name [$clusterName] is not running")))

  /**
    * @param key cluster key
    * @return true if the cluster exists
    */
  def exist(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Boolean] =
    clusters().map(_.exists(_._1.key == key))

  /**
    * @param key cluster key
    * @return true if the cluster doesn't exist
    */
  def nonExist(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Boolean] =
    exist(key).map(!_)

  /**
    * remove a node from a running cluster.
    * NOTED: this is a async operation since graceful downing a node from a running service may be slow.
    * @param key cluster key
    * @param nodeName node name
    * @return true if it does remove a node from a running cluster. Otherwise, false
    */
  final def removeNode(key: ObjectKey, nodeName: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    clusters().flatMap(
      _.find(_._1.key == key)
        .filter(_._1.nodeNames.contains(nodeName))
        .filter(_._2.exists(_.nodeName == nodeName))
        .fold(Future.successful(false)) {
          case (cluster, runningContainers) =>
            runningContainers.size match {
              case 1 =>
                Future.failed(new IllegalArgumentException(
                  s"cluster [$key] is a single-node cluster. You can't remove the last node by removeNode(). Please use remove(clusterName) instead"))
              case _ =>
                doRemoveNode(
                  cluster,
                  runningContainers
                    .find(_.nodeName == nodeName)
                    .getOrElse(throw new IllegalArgumentException(
                      s"This should not be happen!!! $nodeName doesn't exist on cluster:$key"))
                )
            }
        })

  /**
    * do the remove actually. Normally, the sub-class doesn't need to check the existence of removed node.
    * @param previousCluster previous cluster
    * @param beRemovedContainer the container to be removed
    * @param executionContext thread pool
    * @return true if success. otherwise, false
    */
  protected def doRemoveNode(previousCluster: T, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean]

  /**
    * Get the cluster state by containers.
    * <p>
    * Note: we should separate the implementation from docker and k8s environment.
    * <p>
    * a cluster state machine:
    *       -----------------       -----------------       --------
    *       | Some(PENDING) |  -->  | Some(RUNNING) |  -->  | None |
    *       -----------------       -----------------       --------
    *                                      |
    *                                      | (terminated failure or running failure)
    *                                      |       ----------------
    *                                      ----->  | Some(FAILED) |
    *                                              ----------------
    * The cluster state rules
    * 1) RUNNING: all of the containers have been created and at least one container is in "running" state
    * 2) FAILED: all of the containers are terminated and at least one container has terminated failure
    * 3) PENDING: one of the containers are in creating phase
    * 4) UNKNOWN: other situations
    * 4) None: no containers
    *
    * @param containers container list
    * @return the cluster state
    */
  protected def toClusterState(containers: Seq[ContainerInfo]): Option[ClusterState]

  /**
    * return the short service name
    * @return service name
    */
  def serviceName: String

  //---------------------------[helper methods]---------------------------//

  /**
    * used to resolve the hostNames.
    * @param hostNames hostNames
    * @return hostname -> ip address
    */
  protected def resolveHostNames(hostNames: Set[String]): Map[String, String] =
    hostNames.map(hostname => hostname -> resolveHostName(hostname)).toMap

  /**
    * used to resolve the hostname.
    * @param hostname hostname
    * @return ip address or hostname (if you do nothing to it)
    */
  protected def resolveHostName(hostname: String): String = CommonUtils.address(hostname)
}

object Collie {

  /**
    * used to distinguish the cluster name and service name
    */
  private[agent] val DIVIDER: String = "-"
  private[agent] val UNKNOWN: String = "unknown"

  private[agent] val LENGTH_OF_CONTAINER_NAME_ID: Int = 7

  /**
    * generate unique name for the container.
    * It can be used in setting container's hostname and name
    * @param prefixKey environment prefix key
    * @param group cluster group
    * @param clusterName cluster name
    * @param serviceName the service type name for current cluster
    * @return a formatted string. form: {prefixKey}-{group}-{clusterName}-{service}-{index}
    */
  def format(prefixKey: String, group: String, clusterName: String, serviceName: String): String =
    Seq(
      prefixKey,
      group,
      clusterName,
      serviceName,
      CommonUtils.randomString(LENGTH_OF_CONTAINER_NAME_ID)
    ).mkString(DIVIDER)

  /**
    * a helper method to fetch the cluster key from container name
    *
    * @param containerName the container runtime name
    */
  private[agent] def objectKeyOfContainerName(containerName: String): ObjectKey =
    // form: PREFIX_KEY-GROUP-CLUSTER_NAME-SERVICE-HASH
    ObjectKey.of(containerName.split(DIVIDER)(1), containerName.split(DIVIDER)(2))

  /**
    * The basic creator that for cluster creation.
    * We define the "required" parameters for a cluster here, and you should fill in each parameter
    * in the individual cluster creation.
    * Note: the checking rules are moved to the api-creation level.
    */
  trait ClusterCreator extends com.island.ohara.common.pattern.Creator[Future[Unit]] with ClusterRequest {

    protected var executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

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

    /**
      * submit a creation progress in background. the implementation should avoid creating duplicate containers on the
      * same nodes. If the pass nodes already have containers, this creation should be viewed as "adding" than creation.
      * for example, the cluster-A exists and it is running on node-01. When user pass a creation to run cluster-A on
      * node-02, the creation progress should be aware of that user tries to add a new node (node-02) to the cluster-A.
      */
    override def create(): Future[Unit]
  }
}
