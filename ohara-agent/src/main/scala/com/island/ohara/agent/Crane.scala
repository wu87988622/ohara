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
import java.util.concurrent.{ExecutorService, Executors}

import com.island.ohara.agent.docker.DockerCraneImpl
import com.island.ohara.agent.k8s.{K8SClient, K8sCraneImpl}
import com.island.ohara.agent.ssh.DockerClientCache
import com.island.ohara.agent.wharf.StreamWarehouse
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.common.util.Releasable

import scala.concurrent.{ExecutionContext, Future}

/**
  * Crane is a machine that can lift materials to other places horizontally.
  * This is the top-of-the-range "warehouse". It maintains and organizes all warehouses.
  */
trait Crane extends Releasable {

  /**
    * remove warehouse by specified name.
    *
    * @param warehouseName warehouse name
    * @param executionContext execution context
    * @return removed cluster information, exception if not exists
    */
  def remove(warehouseName: String)(implicit executionContext: ExecutionContext): Future[ClusterInfo]

  /**
    * list all warehouses
    *
    * @param executionContext execution context
    * @return container cluster list
    */
  def list(implicit executionContext: ExecutionContext): Future[Map[ClusterInfo, Seq[ContainerInfo]]]

  /**
    * get warehouse by name
    *
    * @param warehouseName warehouse name
    * @param executionContext execution context
    * @return container cluster, or throw exception if not exist
    */
  def get(warehouseName: String)(implicit executionContext: ExecutionContext): Future[(ClusterInfo, Seq[ContainerInfo])]

  /**
    * check the warehouse exists or not
    *
    * @param warehouseName warehouse name
    * @param executionContext execution context
    * @return true if exists
    */
  def exist(warehouseName: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    list.map(_.exists(_._1.name == warehouseName))

  def streamWarehouse(): StreamWarehouse
}

object Crane {

  def builderOfDocker(): DockerBuilder = new DockerBuilder

  def builderOfK8s(): K8sBuilder = new K8sBuilder

  private[agent] class DockerBuilder {
    private[this] var nodeCollie: NodeCollie = _
    private[this] var dockerClientCache: DockerClientCache = _
    private[this] var executor: ExecutorService = _

    /**
      * Set the Crane "control" nodes
      * This implies all the warehouses that will be added to this crane
      * should use subset of nodes
      *
      * @param nodeCollie nodeCollie
      * @return this builder
      */
    def nodeCollie(nodeCollie: NodeCollie): DockerBuilder = {
      this.nodeCollie = Objects.requireNonNull(nodeCollie)
      this
    }

    /**
      * set the docker client cache (ex: `DockerClientCache()`)
      *
      * @param dockerClientCache docker client cache
      * @return this builder
      */
    def dockerClientCache(dockerClientCache: DockerClientCache): DockerBuilder = {
      this.dockerClientCache = Objects.requireNonNull(dockerClientCache)
      this
    }

    /**
      * set a thread pool that initial size is equal with number of cores
      * @return this builder
      */
    def executorDefault(): DockerBuilder = executor(
      Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors()))

    def executor(executor: ExecutorService): DockerBuilder = {
      this.executor = Objects.requireNonNull(executor)
      this
    }

    /**
      * build this stream crane
      *
      * @return crane object
      */
    def build(): Crane = new DockerCraneImpl(
      nodeCollie = Objects.requireNonNull(nodeCollie),
      dockerCache = Objects.requireNonNull(dockerClientCache),
      executor = Objects.requireNonNull(executor)
    )
  }

  private[agent] class K8sBuilder {
    private[this] var nodeCollie: NodeCollie = _
    private[this] var k8sClient: K8SClient = _

    /**
      * Set the Crane "control" nodes
      * This implies all the warehouses that will be added to this crane
      * should use subset of nodes
      *
      * @param nodeCollie nodeCollie
      * @return this builder
      */
    def nodeCollie(nodeCollie: NodeCollie): K8sBuilder = {
      this.nodeCollie = Objects.requireNonNull(nodeCollie)
      this
    }

    /**
      * Set the k8sClient for Crane
      *
      * @param k8sClient k8sClient
      * @return this builder
      */
    def k8sClient(k8sClient: K8SClient): K8sBuilder = {
      this.k8sClient = Objects.requireNonNull(k8sClient)
      this
    }

    /**
      * build this stream crane
      *
      * @return crane object
      */
    def build(): Crane = new K8sCraneImpl(
      nodeCollie = Objects.requireNonNull(nodeCollie),
      k8sClient = Objects.requireNonNull(k8sClient)
    )
  }
}
