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

package com.island.ohara.configurator.store

import com.island.ohara.client.configurator.v0.ClusterInfo

case class FakeClusterInfo(name: String) extends ClusterInfo {

  override def imageName: String = "I DON'T CARE"

  override def ports: Set[Int] = Set.empty

  override def nodeNames: Set[String] = Set.empty

  // we should NOT use this method in testing
  override def clone(newNodeNames: Set[String]): FakeClusterInfo = throw new UnsupportedOperationException(
    "what are you doing!!!")
}
