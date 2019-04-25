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

package com.island.ohara.agent.wharf

import com.island.ohara.agent.wharf.Warehouse.WarehouseCreator
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.common.util.CommonUtils

import scala.concurrent.{ExecutionContext, Future}

/**
  * Warehouse is a factory to manage containers.
  * This class is as equal as a container cluster.
  * NOTED: since collie is used for two impl (ssh, k8s) and will refactor in the future,
  * we need to decouple "worker cluster" and "container cluster" for further usage.
  * TODO : need to refactor this after k8s refactor done..by Sam
  */
trait Warehouse[T <: ClusterInfo] {

  /**
    * Get the number of the containers
    *
    * @param executionContext execution context
    * @return size of container
    */
  def size(clusterName: String)(implicit executionContext: ExecutionContext): Future[Int] =
    containers(clusterName).map(_.size)

  /**
    * Get all containers from this cluster
    *
    * @param executionContext execution context
    * @return containers information
    */
  def containers(clusterName: String)(implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]]

  /**
    * create a warehouse creator
    * @return creator of warehouse
    */
  def creator(): WarehouseCreator[T]

  /**
    * format the container name
    * format : {prefix}-{serviceName}-{clusterName}-{randomId}
    *
    * @param serviceName service name (ex : stream)
    * @param clusterName cluster name
    * @return formatted string
    */
  protected def format(serviceName: String, clusterName: String): String = {
    Seq(
      Warehouse.PREFIX_KEY,
      serviceName,
      clusterName,
      CommonUtils.randomString(Warehouse.LENGTH_OF_CONTAINER_NAME_ID)
    ).mkString(Warehouse.DIVIDER)
  }
}

object Warehouse {

  final val LENGTH_OF_CONTAINER_NAME_ID: Int = 7
  final val PREFIX_KEY: String = "osw"
  final val DIVIDER: String = "-"

  // k8s default
  val K8S_DOMAIN_NAME: String = "default"
  val OHARA_LABEL: String = "ohara"

  /**
    * docker does limit the length of name (< 64). So we release only half length (32 chars) to user.
    */
  final val LIMIT_OF_NAME_LENGTH: Int = 32

  trait WarehouseCreator[T <: ClusterInfo] {

    protected var imageName: String = _
    protected var clusterName: String = _

    private[this] def assertLength(s: String): String = if (s.length > LIMIT_OF_NAME_LENGTH)
      throw new IllegalArgumentException(s"limit of length is $LIMIT_OF_NAME_LENGTH. actual: ${s.length}")
    else s

    /**
      * set the image name used to create cluster's container
      * @param imageName image name
      * @return this creator
      */
    def imageName(imageName: String): WarehouseCreator.this.type = {
      this.imageName = CommonUtils.requireNonEmpty(imageName)
      this
    }

    /**
      * set the cluster name
      * @param clusterName cluster name
      * @return this creator
      */
    def clusterName(clusterName: String): WarehouseCreator.this.type = {
      this.clusterName = assertLength(CommonUtils.assertOnlyNumberAndChar(clusterName))
      this
    }

    def create()(implicit executionContext: ExecutionContext): Future[T]
  }
}
