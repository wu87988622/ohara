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
import akka.http.scaladsl.server
import com.island.ohara.agent.{BrokerCollie, NoSuchClusterException}
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.MetricsApi.{Meter, Metrics}
import com.island.ohara.client.configurator.v0.TopicApi._
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.route.RouteUtils._
import com.island.ohara.configurator.store.DataStore
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}
private[configurator] object TopicRoute {
  private[this] val DEFAULT_NUMBER_OF_PARTITIONS: Int = 1
  private[this] val DEFAULT_NUMBER_OF_REPLICATIONS: Short = 1
  private[this] val LOG = Logger(TopicRoute.getClass)

  /**
    * fetch the topic meters from broker cluster
    * @param brokerCluster the broker cluster hosting the topic
    * @param topicName topic name which used to filter the correct meter
    * @param brokerCollie broker collie
    * @return meters belong to the input topic
    */
  private[this] def metrics(brokerCluster: BrokerClusterInfo, topicName: String)(
    implicit brokerCollie: BrokerCollie): Metrics = Metrics(
    brokerCollie.topicMeters(brokerCluster).filter(_.topicName() == topicName).map { meter =>
      Meter(
        value = meter.count(),
        unit = s"${meter.eventType()} / ${meter.rateUnit().name()}",
        document = meter.catalog.name()
      )
    })

  /**
    * update the metrics for input topic
    * @param brokerCluster the broker cluster hosting the topic
    * @param topicInfo topic info
    * @param brokerCollie broker collie
    * @return updated topic info
    */
  private[this] def update(brokerCluster: BrokerClusterInfo, topicInfo: TopicInfo)(
    implicit brokerCollie: BrokerCollie): TopicInfo = topicInfo.copy(
    metrics = metrics(brokerCluster, topicInfo.id)
  )

  /**
    * TODO: remove TargetCluster. see https://github.com/oharastream/ohara/issues/206
    */
  private[this] def updateBrokerClusterName(request: TopicCreationRequest, t: TargetCluster): TopicCreationRequest =
    if (request.brokerClusterName.isEmpty) request.copy(brokerClusterName = t) else request

  private[this] def hookOfAdd(id: Id, request: TopicCreationRequest)(
    implicit brokerCollie: BrokerCollie,
    executionContext: ExecutionContext): Future[TopicInfo] =
    request.name
      .map { name =>
        CollieUtils.topicAdmin(request.brokerClusterName).flatMap {
          case (cluster, client) =>
            client
              .creator()
              // NOTED: we allow user to change topic's name arbitrarily
              .name(id)
              .numberOfPartitions(request.numberOfPartitions.getOrElse(DEFAULT_NUMBER_OF_PARTITIONS))
              .numberOfReplications(request.numberOfReplications.getOrElse(DEFAULT_NUMBER_OF_REPLICATIONS))
              .create()
              .map { info =>
                try TopicInfo(
                  id,
                  name,
                  info.numberOfPartitions,
                  info.numberOfReplications,
                  cluster.name,
                  // the topic is just created so we don't fetch the "empty" metrics actually.
                  metrics = Metrics(Seq.empty),
                  CommonUtils.current()
                )
                finally client.close()
              }
        }
      }
      .getOrElse(Future.failed(new NoSuchElementException(s"name is required")))

  def apply(implicit store: DataStore, brokerCollie: BrokerCollie, executionContext: ExecutionContext): server.Route =
    RouteUtils.basicRoute[TopicCreationRequest, TopicInfo](
      root = TOPICS_PREFIX_PATH,
      hookOfAdd = (targetCluster: TargetCluster, id: Id, request: TopicCreationRequest) =>
        hookOfAdd(id, updateBrokerClusterName(request, targetCluster)),
      hookOfUpdate = (id: Id, request: TopicCreationRequest, previous: TopicInfo) =>
        CollieUtils.topicAdmin(Some(previous.brokerClusterName)).flatMap {
          case (cluster, client) =>
            val requestNumberOfPartitions = request.numberOfPartitions.getOrElse(previous.numberOfPartitions)
            val requestNumberOfReplications = request.numberOfReplications.getOrElse(previous.numberOfReplications)
            if (requestNumberOfReplications != previous.numberOfReplications) {
              // we have got to release the client
              Releasable.close(client)
              Future.failed(new IllegalArgumentException("Non-support to change the number from replications"))
            } else if (requestNumberOfPartitions > previous.numberOfPartitions)
              client.changePartitions(id, request.numberOfPartitions.get).map { info =>
                try TopicInfo(
                  info.name,
                  request.name.getOrElse(previous.name),
                  info.numberOfPartitions,
                  info.numberOfReplications,
                  cluster.name,
                  metrics = metrics(cluster, info.name),
                  CommonUtils.current()
                )
                finally client.close()
              } else if (requestNumberOfPartitions < previous.numberOfPartitions) {
              Releasable.close(client)
              Future.failed(new IllegalArgumentException("Reducing the number from partitions is disallowed"))
            } else {
              // we have got to release the client
              Releasable.close(client)
              Future.successful(request.name.map(n => previous.copy(name = n)).getOrElse(previous))
            }
      },
      hookOfDelete = (response: TopicInfo) =>
        CollieUtils
          .topicAdmin(Some(response.brokerClusterName))
          .flatMap {
            case (_, client) =>
              client
                .delete(response.id)
                .map { _ =>
                  try response
                  finally Releasable.close(client)
                }
                .recover {
                  case e: Throwable =>
                    LOG.error(s"failed to remove topic:${response.id} from kafka", e)
                    response
                }
          }
          .recover {
            case e: NoSuchClusterException =>
              LOG.warn(
                s"the cluster:${response.brokerClusterName} doesn't exist!!! just remove topic from configurator",
                e)
              response
        },
      hookOfGet = (response: TopicInfo) =>
        brokerCollie.cluster(response.brokerClusterName).map {
          case (cluster, _) => update(cluster, response)
      },
      hookOfList = (responses: Seq[TopicInfo]) =>
        Future.traverse(responses) { response =>
          brokerCollie.cluster(response.brokerClusterName).map {
            case (cluster, _) => update(cluster, response)
          }
      }
    )
}
