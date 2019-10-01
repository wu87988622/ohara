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

import com.island.ohara.agent.{ServiceCollie, NodeCollie}
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.{BrokerApi, StreamApi, WorkerApi, ZookeeperApi}
import com.island.ohara.configurator.store.DataStore

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * It doesn't involve any running cluster but save all description in memory
  */
private[configurator] class FakeServiceCollie(nodeCollie: NodeCollie,
                                              store: DataStore,
                                              bkConnectionProps: String,
                                              wkConnectionProps: String)
    extends ServiceCollie {

  def this(nodeCollie: NodeCollie, store: DataStore) {
    this(nodeCollie, store, null, null)
  }

  override val zookeeperCollie: FakeZookeeperCollie = new FakeZookeeperCollie(nodeCollie)

  override val brokerCollie: FakeBrokerCollie = new FakeBrokerCollie(nodeCollie, bkConnectionProps)

  override val workerCollie: FakeWorkerCollie = new FakeWorkerCollie(nodeCollie, wkConnectionProps)

  override val streamCollie: FakeStreamCollie = new FakeStreamCollie(nodeCollie)

  override def close(): Unit = {
    // do nothing
  }

  override def images(nodes: Seq[Node])(implicit executionContext: ExecutionContext): Future[Map[Node, Seq[String]]] =
    Future.successful(
      nodes
        .map(
          _ -> Seq(ZookeeperApi.IMAGE_NAME_DEFAULT,
                   BrokerApi.IMAGE_NAME_DEFAULT,
                   WorkerApi.IMAGE_NAME_DEFAULT,
                   StreamApi.IMAGE_NAME_DEFAULT))
        .toMap)

  override def verifyNode(node: Node)(implicit executionContext: ExecutionContext): Future[Try[String]] =
    Future.successful(Try(s"This is fake mode so we didn't test connection actually..."))
}
