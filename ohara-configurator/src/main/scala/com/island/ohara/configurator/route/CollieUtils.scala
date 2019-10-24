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

package com.island.ohara.configurator.route
import com.island.ohara.agent._
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.kafka.{TopicAdmin, WorkerClient}
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.configurator.store.DataStore

import scala.concurrent.{ExecutionContext, Future}

/**
  * TODO: this is just a workaround in ohara 0.3. It handles the following trouble:
  * 1) UI doesn't support user to host zk and bk cluster so most request won't carry the information of broker. Hence
  *    we need to handle the request with "default" broker cluster
  * 2) make ops to cluster be "blocking"
  */
private[route] object CollieUtils {

  /**
    * Create a topic admin according to passed cluster name.
    * Noted: if target cluster doesn't exist, an future with exception will return
    * @param clusterKey target cluster
    * @return cluster info and topic admin
    */
  def topicAdmin(clusterKey: ObjectKey)(implicit brokerCollie: BrokerCollie,
                                        store: DataStore,
                                        cleaner: AdminCleaner,
                                        executionContext: ExecutionContext): Future[(BrokerClusterInfo, TopicAdmin)] =
    store
      .value[BrokerClusterInfo](clusterKey)
      .flatMap(cluster => brokerCollie.topicAdmin(cluster).map(cleaner.add).map(cluster -> _))

  /**
    * Create a worker client according to passed cluster name.
    * Noted: if target cluster doesn't exist, an future with exception will return
    * @param clusterKey target cluster
    * @return cluster info and client
    */
  def workerClient(clusterKey: ObjectKey)(
    implicit workerCollie: WorkerCollie,
    store: DataStore,
    executionContext: ExecutionContext): Future[(WorkerClusterInfo, WorkerClient)] =
    store.value[WorkerClusterInfo](clusterKey).flatMap(cluster => workerCollie.workerClient(cluster).map(cluster -> _))

  def both[T](workerClusterKey: ObjectKey)(
    implicit brokerCollie: BrokerCollie,
    store: DataStore,
    cleaner: AdminCleaner,
    workerCollie: WorkerCollie,
    executionContext: ExecutionContext): Future[(BrokerClusterInfo, TopicAdmin, WorkerClusterInfo, WorkerClient)] =
    workerClient(workerClusterKey).flatMap {
      case (wkInfo, wkClient) =>
        topicAdmin(wkInfo.brokerClusterKey).map {
          case (bkInfo, topicAdmin) => (bkInfo, cleaner.add(topicAdmin), wkInfo, wkClient)
        }
    }

}
