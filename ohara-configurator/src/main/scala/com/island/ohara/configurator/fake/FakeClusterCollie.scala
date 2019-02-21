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

package com.island.ohara.configurator.fake

import com.island.ohara.agent.ClusterCollie
import com.island.ohara.client.configurator.v0.NodeApi.{Node, NodeService}
import com.island.ohara.client.configurator.v0.{BrokerApi, WorkerApi, ZookeeperApi}
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store

import scala.concurrent.Future

/**
  * It doesn't involve any running cluster but save all description in memory
  */
private[configurator] class FakeClusterCollie(store: Store, bkConnectionProps: String, wkConnectionProps: String)
    extends ClusterCollie {

  def this(store: Store) {
    this(store, null, null)
  }
  private[this] val zkCollie: FakeZookeeperCollie = new FakeZookeeperCollie
  private[this] val bkCollie: FakeBrokerCollie = new FakeBrokerCollie(bkConnectionProps)
  private[this] val wkCollie: FakeWorkerCollie = new FakeWorkerCollie(wkConnectionProps)

  override def zookeeperCollie(): FakeZookeeperCollie = zkCollie

  override def brokerCollie(): FakeBrokerCollie = bkCollie

  override def workerCollie(): FakeWorkerCollie = wkCollie

  override def close(): Unit = {
    // do nothing
  }

  override protected def update(node: Node, services: Seq[NodeService]): Node = FakeNode(
    name = node.name,
    port = node.port,
    user = node.user,
    password = node.password,
    services = services,
    lastModified = CommonUtil.current()
  )

  override def images(nodes: Seq[Node]): Future[Map[Node, Seq[String]]] = Future.successful(
    nodes
      .map(_ -> Seq(ZookeeperApi.IMAGE_NAME_DEFAULT, BrokerApi.IMAGE_NAME_DEFAULT, WorkerApi.IMAGE_NAME_DEFAULT))
      .toMap)
}
