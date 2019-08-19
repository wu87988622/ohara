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
import com.island.ohara.agent.{BrokerCollie, Collie, WorkerCollie}
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.kafka.{TopicAdmin, WorkerClient}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * TODO: this is just a workaround in ohara 0.3. It handles the following trouble:
  * 1) UI doesn't support user to host zk and bk cluster so most request won't carry the information of broker. Hence
  *    we need to handle the request with "default" broker cluster
  * 2) make ops to cluster be "blocking"
  */
private[route] object CollieUtils {

  def topicAdmin(clusterName: String)(implicit brokerCollie: BrokerCollie,
                                      cleaner: AdminCleaner,
                                      executionContext: ExecutionContext): Future[(BrokerClusterInfo, TopicAdmin)] =
    topicAdmin(Some(clusterName))

  /**
    * find the broker cluster info. If the input cluster name is empty, the single broker cluster is returned. Otherwise,
    * an exception is thrown.
    * @param clusterName broker cluster name
    * @param brokerCollie broker collie
    * @param cleaner cleaner
    * @param executionContext thread pool
    * @return broker cluster and topic admin
    */
  private[this] def topicAdmin(clusterName: Option[String])(implicit brokerCollie: BrokerCollie,
                                                            cleaner: AdminCleaner,
                                                            executionContext: ExecutionContext)
    : Future[(BrokerClusterInfo, TopicAdmin)] = clusterName.fold(brokerCollie.clusters
    .map { clusters =>
      clusters.size match {
        case 0 =>
          throw new IllegalArgumentException(s"we can't choose default broker cluster since there is no broker cluster")
        case 1 => clusters.keys.head
        case _ =>
          throw new IllegalArgumentException(
            s"we can't choose default broker cluster since there are too many broker cluster:${clusters.keys.map(_.name).mkString(",")}")
      }
    }
    .map(clusterInfo => (clusterInfo, cleaner.add(brokerCollie.topicAdmin(clusterInfo)))))(brokerCollie.topicAdmin)

  /**
    * The mechanism has three phases.
    * 1) return the cluster name if there is only one running cluster
    * 2) finally, throw exception to remind caller that server fails to do auto-completion for property
    * @param collie collie
    * @param executionContext thread pool
    * @tparam Req cluster type
    * @return matched cluster name
    */
  def singleCluster[Req <: ClusterInfo: ClassTag]()(implicit collie: Collie[Req],
                                                    executionContext: ExecutionContext): Future[String] =
    collie.clusters().map { clusters =>
      clusters.size match {
        case 0 =>
          throw new IllegalArgumentException(s"we can't choose default cluster since there is no cluster available")
        case 1 => clusters.keys.head.name
        case _ =>
          throw new IllegalArgumentException(
            s"we can't choose default cluster since there are too many clusters:${clusters.keys.map(_.name).mkString(",")}")
      }
    }

  def workerClient[T](clusterName: String)(
    implicit workerCollie: WorkerCollie,
    executionContext: ExecutionContext): Future[(WorkerClusterInfo, WorkerClient)] = workerClient(Some(clusterName))

  private[this] def workerClient[T](clusterName: Option[String])(
    implicit workerCollie: WorkerCollie,
    executionContext: ExecutionContext): Future[(WorkerClusterInfo, WorkerClient)] = clusterName
    .map(workerCollie.workerClient)
    .getOrElse(workerCollie.clusters
      .map { clusters =>
        clusters.size match {
          case 0 =>
            throw new IllegalArgumentException(
              s"we can't choose default worker cluster since there is no worker cluster")
          case 1 => clusters.keys.head
          case _ =>
            throw new IllegalArgumentException(
              s"we can't choose default worker cluster since there are too many worker cluster:${clusters.keys.map(_.name).mkString(",")}")
        }
      }
      .map(c => (c, workerCollie.workerClient(c))))

  def both[T](wkClusterName: String)(
    implicit brokerCollie: BrokerCollie,
    cleaner: AdminCleaner,
    workerCollie: WorkerCollie,
    executionContext: ExecutionContext): Future[(BrokerClusterInfo, TopicAdmin, WorkerClusterInfo, WorkerClient)] =
    both(Some(wkClusterName))

  private[this] def both[T](wkClusterName: Option[String])(
    implicit brokerCollie: BrokerCollie,
    cleaner: AdminCleaner,
    workerCollie: WorkerCollie,
    executionContext: ExecutionContext): Future[(BrokerClusterInfo, TopicAdmin, WorkerClusterInfo, WorkerClient)] =
    workerClient(wkClusterName).flatMap {
      case (wkInfo, wkClient) =>
        brokerCollie.topicAdmin(wkInfo.brokerClusterName).map {
          case (bkInfo, topicAdmin) => (bkInfo, cleaner.add(topicAdmin), wkInfo, wkClient)
        }
    }
}
