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
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.util.CommonUtils

import scala.concurrent.{ExecutionContext, Future}

/**
  * Cargo contains all kind of goods and ready to be conveyed.
  * We store the container information in this class.
  */
trait Cargo {
  type T = ContainerInfo

  /**
    * the prefix to identify class
    */
  private[this] val CARGO_KEY = "cargo-"
  private[this] final val _name: String = CARGO_KEY + CommonUtils.randomString()

  /**
    * this cargo unique name
    * @return name
    */
  def name: String = _name

  /**
    * Get the container information
    *
    * @return the container info
    */
  def info(): Future[T]

  /**
    * Remove the container
    *
    * @param executionContext execution context
    * @param isForce should wait the container removing
    * @return true if the cargo was removed, false otherwise
    */
  def removeContainer(isForce: Boolean)(implicit executionContext: ExecutionContext): Future[Boolean]
}

object Cargo {

  /**
    * a builder pattern to create a container inside this cargo
    */
  trait ContainerCreator {
    type T = ContainerInfo

    protected var node: Node = _
    protected var imageName: String = _

    /**
      * set the container image name
      *
      * @param imageName image name
      * @return this builder
      */
    def imageName(imageName: String): ContainerCreator.this.type = {
      this.imageName = CommonUtils.requireNonEmpty(imageName)
      this
    }

    /**
      * Create the cargo by required information
      *
      * @param executionContext execution context
      * @return container information
      */
    def create()(implicit executionContext: ExecutionContext): Future[T]
  }
}
