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
import scala.concurrent.{ExecutionContext, Future}

/**
  * Crane is a machine that can lift materials to other places horizontally.
  * This is the interface for managing and deploying warehouses (container groups).
  *
  */
trait Crane {

  /**
    * List all warehouses that are contained in this crane
    *
    * @param executionContext execution context
    * @return warehouse list
    */
  def list()(implicit executionContext: ExecutionContext): Future[Seq[Warehouse]]

  /**
    * Get specific warehouse
    *
    * @param name warehouse name
    * @param executionContext execution context
    * @return the required warehouse, or throw exception if not exists
    */
  def get(name: String)(implicit executionContext: ExecutionContext): Future[Warehouse] =
    list().map(
      _.find(_.name == name)
        .getOrElse(throw new IllegalArgumentException(s"no such warehouse by require name : $name")))

  /**
    * Add a warehouse with required size (the cargoes and dependent containers will be created also)
    *
    * @param size the size of warehouse
    * @param executionContext execution context
    * @return the warehouse that is successfully added, or previous exists warehouse
    */
  def add(size: Int)(implicit executionContext: ExecutionContext): Future[(String, Warehouse)]

  /**
    * Delete a warehouse
    *
    * @param name warehouse name
    * @param executionContext execution context
    * @return the deleted cargo group, or throw exception if not exists
    */
  def delete(name: String)(implicit executionContext: ExecutionContext): Future[Warehouse]
}
