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
import com.island.ohara.common.util.CommonUtils

import scala.concurrent.{ExecutionContext, Future}

/**
  * Warehouse is a factory to store all cargoes.
  * This class is a cargo group (container group).
  */
trait Warehouse {

  /**
    * the prefix to identify class
    */
  private[this] val WAREHOUSE_KEY = "warehouse-"
  private[this] final val _name: String = WAREHOUSE_KEY + CommonUtils.randomString()

  /**
    * The unique name for this warehouse
    *
    * @return this warehouse name
    */
  def name: String = _name

  /**
    * Get all cargoes from this warehouse
    *
    * @param executionContext execution context
    * @return cargo list
    */
  def list()(implicit executionContext: ExecutionContext): Future[Seq[Cargo]]

  /**
    * Get the specific cargo
    *
    * @param cargoName the required cargo name
    * @param executionContext execution context
    * @return the require cargo, or None if not found
    */
  def get(cargoName: String)(implicit executionContext: ExecutionContext): Future[Option[Cargo]] =
    list().map(_.find(_.name == cargoName))

  /**
    * Add a cargo to this warehouse (should also create container)
    *
    * @param nodeName the node to be added cargo
    * @param executionContext execution context
    * @return value of the added cargo if not exists, or the previous value
    */
  def add(nodeName: String)(implicit executionContext: ExecutionContext): Future[Cargo]

  /**
    * Delete cargo by it's name
    *
    * @param cargoName the cargo name that is want to be deleted
    * @param executionContext execution context
    * @return the deleted cargo, or exception if no such cargo
    */
  def delete(cargoName: String)(implicit executionContext: ExecutionContext): Future[Cargo]
}
