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

package com.island.ohara.client.configurator.v0

import com.island.ohara.common.setting.ObjectKey

/**
  * the information of running cluster. Those information may be changed over time.
  */
trait ClusterStatus {

  def group: String

  def name: String

  /**
    * a helper method used to generate the key of this data.
    * @return key
    */
  def key: ObjectKey = ObjectKey.of(group, name)

  /**
    * @return the state of this cluster. None means the cluster is not running
    */
  def state: Option[String]

  /**
    * the nodes do run the containers of cluster.
    * @return a collection of node names
    */
  def aliveNodes: Set[String]

  /**
    * @return the error message of this cluster.
    */
  def error: Option[String]
}
