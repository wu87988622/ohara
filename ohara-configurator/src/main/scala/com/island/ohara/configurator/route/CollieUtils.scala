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
import com.island.ohara.agent.{BrokerCollie, WorkerCollie}
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.kafka.BrokerClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * TODO: this is just a workaround in ohara 0.2. It handles the following trouble:
  * 1) UI doesn't support user to host zk and bk cluster so most request won't carry the information of broker. Hence
  *    we need to handle the request with "default" broker cluster
  * 2) make ops to cluster be "blocking"
  */
object CollieUtils {

  private[route] def brokerClient[T](clusterName: Option[String])(
    implicit brokerCollie: BrokerCollie): Future[(BrokerClusterInfo, BrokerClient)] = clusterName
    .map(Future.successful)
    .getOrElse(brokerCollie.clusters().map { clusters =>
      clusters.size match {
        case 0 =>
          throw new IllegalArgumentException(
            s"we can't use default zookeeper cluster since there is no zookeeper cluster")
        case 1 => clusters.keys.head.name
        case _ =>
          throw new IllegalArgumentException(
            s"we can't use default zookeeper cluster since there are too many zookeeper cluster:${clusters.keys.map(_.name).mkString(",")}")
      }
    })
    .flatMap(brokerCollie.createClient)

  private[route] def workerClient[T](clusterName: Option[String])(
    implicit workerCollie: WorkerCollie): Future[(WorkerClusterInfo, WorkerClient)] = clusterName
    .map(Future.successful)
    .getOrElse(workerCollie.clusters().map { clusters =>
      clusters.size match {
        case 0 =>
          throw new IllegalArgumentException(s"we can't use default broker cluster since there is no broker cluster")
        case 1 => clusters.keys.head.name
        case _ =>
          throw new IllegalArgumentException(
            s"we can't use default broker cluster since there are too many broker cluster:${clusters.keys.map(_.name).mkString(",")}")
      }
    })
    .flatMap(workerCollie.createClient)

  private[route] def bothClient[T](wkClusterName: Option[String])(
    implicit brokerCollie: BrokerCollie,
    workerCollie: WorkerCollie): Future[(BrokerClusterInfo, BrokerClient, WorkerClusterInfo, WorkerClient)] =
    workerClient(wkClusterName).flatMap {
      case (wkInfo, wkClient) =>
        brokerCollie.createClient(wkInfo.brokerClusterName).map {
          case (bkInfo, bkClient) => (bkInfo, bkClient, wkInfo, wkClient)
        }
    }
}
