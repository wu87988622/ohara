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

package com.island.ohara.it

import com.island.ohara.client.configurator.v0.{BrokerApi, WorkerApi, ZookeeperApi}
import com.island.ohara.common.setting.ObjectKey
import org.junit.Before

import scala.concurrent.ExecutionContext.Implicits.global

abstract class WithRemoteWorkers extends WithRemoteConfigurator {
  protected val zkKey: ObjectKey = serviceNameHolder.generateClusterKey()
  private[this] def zkApi =
    ZookeeperApi.access
      .hostname(configuratorHostname)
      .port(configuratorPort)

  protected val bkKey: ObjectKey = serviceNameHolder.generateClusterKey()
  private[this] def bkApi =
    BrokerApi.access
      .hostname(configuratorHostname)
      .port(configuratorPort)
  protected def brokerConnection: String = result(bkApi.get(bkKey)).connectionProps

  protected val wkKey: ObjectKey = serviceNameHolder.generateClusterKey()
  private[this] def wkApi =
    WorkerApi.access
      .hostname(configuratorHostname)
      .port(configuratorPort)
  protected def workerConnection: String = result(wkApi.get(wkKey)).connectionProps

  @Before
  def setupWorkers(): Unit = {
    // single zk
    result(
      zkApi.request
        .key(zkKey)
        .nodeName(nodeNames.head)
        .create()
        .map(_.key)
        .flatMap(zkApi.start)
    )
    await(() => result(zkApi.get(zkKey)).state.isDefined)

    result(
      bkApi.request
        .key(bkKey)
        .zookeeperClusterKey(zkKey)
        .nodeNames(nodeNames.toSet)
        .create()
        .map(_.key)
        .flatMap(bkApi.start)
    )
    await(() => result(bkApi.get(bkKey)).state.isDefined)

    result(
      wkApi.request
        .key(wkKey)
        .brokerClusterKey(bkKey)
        .nodeNames(nodeNames.toSet)
        .create()
        .map(_.key)
        .flatMap(wkApi.start)
    )
    await(() => result(wkApi.get(wkKey)).state.isDefined)
  }
}
