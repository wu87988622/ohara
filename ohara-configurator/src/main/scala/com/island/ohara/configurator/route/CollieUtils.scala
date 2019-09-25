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
import com.island.ohara.agent.{BrokerCollie, Collie, WorkerCollie, ZookeeperCollie}
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.client.kafka.{TopicAdmin, WorkerClient}
import com.island.ohara.common.setting.ObjectKey

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * TODO: this is just a workaround in ohara 0.3. It handles the following trouble:
  * 1) UI doesn't support user to host zk and bk cluster so most request won't carry the information of broker. Hence
  *    we need to handle the request with "default" broker cluster
  * 2) make ops to cluster be "blocking"
  */
private[route] object CollieUtils {

  // TODO: remove this stale method
  def topicAdmin(clusterName: String)(implicit brokerCollie: BrokerCollie,
                                      cleaner: AdminCleaner,
                                      executionContext: ExecutionContext): Future[(BrokerClusterInfo, TopicAdmin)] =
    topicAdmin(ObjectKey.of(GROUP_DEFAULT, clusterName))

  /**
    * Create a topic admin according to passed cluster name.
    * Noted: if target cluster doesn't exist, an future with exception will return
    * @param clusterKey target cluster
    * @return cluster info and topic admin
    */
  def topicAdmin(clusterKey: ObjectKey)(implicit brokerCollie: BrokerCollie,
                                        cleaner: AdminCleaner,
                                        executionContext: ExecutionContext): Future[(BrokerClusterInfo, TopicAdmin)] =
    brokerCollie.cluster(clusterKey).map {
      case (c, _) => (c, cleaner.add(brokerCollie.topicAdmin(c)))
    }

  /**
    * find the single running zookeeper cluster. Otherwise, it throws exception
    * @param collie zookeeper collie
    * @param executionContext thread pool
    * @return key of single running zookeeper cluster
    */
  def singleZookeeperCluster()(implicit collie: ZookeeperCollie,
                               executionContext: ExecutionContext): Future[ObjectKey] =
    singleCluster[ZookeeperClusterInfo]

  /**
    * find the single running broker cluster. Otherwise, it throws exception
    * @param collie broker collie
    * @param executionContext thread pool
    * @return key of single running broker cluster
    */
  def singleBrokerCluster()(implicit collie: BrokerCollie, executionContext: ExecutionContext): Future[ObjectKey] =
    singleCluster[BrokerClusterInfo]

  /**
    * find the single running worker cluster. Otherwise, it throws exception
    * @param collie worker collie
    * @param executionContext thread pool
    * @return key of single running worker cluster
    */
  def singleWorkerCluster()(implicit collie: WorkerCollie, executionContext: ExecutionContext): Future[ObjectKey] =
    singleCluster[WorkerClusterInfo]

  /**
    * The mechanism has three phases.
    * 1) return the cluster name if there is only one running cluster
    * 2) finally, throw exception to remind caller that server fails to do auto-completion for property
    * @param collie collie
    * @param executionContext thread pool
    * @tparam Req cluster type
    * @return matched cluster name
    */
  private[this] def singleCluster[Req <: ClusterInfo: ClassTag]()(
    implicit collie: Collie[Req],
    executionContext: ExecutionContext): Future[ObjectKey] =
    collie.clusters().map { clusters =>
      clusters.size match {
        case 0 =>
          throw new IllegalArgumentException(s"we can't choose default cluster since there is no cluster available")
        case 1 => clusters.keys.head.key
        case _ =>
          throw new IllegalArgumentException(
            s"we can't choose default cluster since there are too many clusters:${clusters.keys.map(_.name).mkString(",")}")
      }
    }

  /**
    * Create a worker client according to passed cluster name.
    * Noted: if target cluster doesn't exist, an future with exception will return
    * @param clusterKey target cluster
    * @return cluster info and client
    */
  def workerClient(clusterKey: ObjectKey)(
    implicit workerCollie: WorkerCollie,
    executionContext: ExecutionContext): Future[(WorkerClusterInfo, WorkerClient)] =
    workerCollie.cluster(clusterKey).map {
      case (c, _) => (c, workerCollie.workerClient(c))
    }

  def both[T](workerClusterKey: ObjectKey)(
    implicit brokerCollie: BrokerCollie,
    cleaner: AdminCleaner,
    workerCollie: WorkerCollie,
    executionContext: ExecutionContext): Future[(BrokerClusterInfo, TopicAdmin, WorkerClusterInfo, WorkerClient)] =
    workerClient(workerClusterKey).flatMap {
      case (wkInfo, wkClient) =>
        topicAdmin(wkInfo.brokerClusterName).map {
          case (bkInfo, topicAdmin) => (bkInfo, cleaner.add(topicAdmin), wkInfo, wkClient)
        }
    }
}
