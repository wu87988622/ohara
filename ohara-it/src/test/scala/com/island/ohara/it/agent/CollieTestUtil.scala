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

package com.island.ohara.it.agent

import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.util.CommonUtil

private[agent] object CollieTestUtil {

  /**
    * form: user:password@hostname:port.
    * NOTED: this key need to be matched with another key value in ohara-it/build.gradle
    */
  val key = "ohara.it.docker"

  def nodeCache(): Seq[Node] = sys.env
    .get(key)
    .map(_.split(",").map { nodeInfo =>
      val user = nodeInfo.split(":").head
      val password = nodeInfo.split("@").head.split(":").last
      val hostname = nodeInfo.split("@").last.split(":").head
      val port = nodeInfo.split("@").last.split(":").last.toInt
      Node(hostname, port, user, password, Seq.empty, CommonUtil.current())
    }.toSeq)
    .getOrElse(Seq.empty)
}
