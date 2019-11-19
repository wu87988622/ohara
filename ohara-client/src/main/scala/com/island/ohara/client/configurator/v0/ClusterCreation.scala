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

/**
  * this is a basic interface of cluster request to create a normal object resource.
  */
trait ClusterCreation extends BasicCreation {
  /**
    * @return nodes to run this cluster
    */
  def nodeNames: Set[String]

  /**
    * @return image used to build cluster
    */
  def imageName: String

  /**
    * @return ports used by this cluster
    */
  def ports: Set[Int]

  /**
    * the port used to expose the jmx service
    * @return jmx port
    */
  def jmxPort: Int
}
