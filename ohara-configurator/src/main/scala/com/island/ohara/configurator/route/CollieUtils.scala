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

  /**
    * create a topic admin and then register it into cleaner
    * @param clusterInfo cluster info
    * @param brokerCollie broker collie
    * @param cleaner cleaner
    * @return topic admin
    */
  def topicAdmin(clusterInfo: BrokerClusterInfo)(implicit brokerCollie: BrokerCollie,
                                                 cleaner: AdminCleaner): TopicAdmin =
    cleaner.add(brokerCollie.topicAdmin(clusterInfo))

  def topicAdmin(clusterName: Option[String])(implicit brokerCollie: BrokerCollie,
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
    .map(c => (c, cleaner.add(topicAdmin(c)))))(brokerCollie.topicAdmin)

  /**
    * The routes, which is based on external system, require property to define cluster name. As a friendly server, those
    * routes offer the auto-completion mechanism to define the cluster for the property lacking of cluster name.
    *
    * The mechanism has three phases.
    * 1) return the cluster name from property if the property has defined the cluster name
    * 2) return the cluster name if there is only one running cluster
    * 3) finally, throw exception to remind caller that server fails to do auto-completion for property
    * @param clusterName cluster name
    * @param collie collie
    * @param executionContext thread pool
    * @tparam Req cluster type
    * @return matched cluster name
    */
  def orElseClusterName[Req <: ClusterInfo: ClassTag](
    clusterName: Option[String])(implicit collie: Collie[Req], executionContext: ExecutionContext): Future[String] =
    if (clusterName.isDefined)
      Future.successful(clusterName.get)
    else
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

  def workerClient[T](clusterName: Option[String])(
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

  def both[T](wkClusterName: Option[String])(
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
