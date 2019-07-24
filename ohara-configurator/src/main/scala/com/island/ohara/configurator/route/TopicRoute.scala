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
import com.island.ohara.client.configurator.v0.DataKey
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.TopicApi._
import com.island.ohara.client.kafka.TopicAdmin
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.route.RouteUtils._
import com.island.ohara.configurator.store.{DataStore, MeterCache}
import com.typesafe.scalalogging.Logger
import spray.json.JsValue

import scala.concurrent.{ExecutionContext, Future}
private[configurator] object TopicRoute {
  private[this] val LOG = Logger(TopicRoute.getClass)

  /**
    * fetch the topic meters from broker cluster
    * @param brokerCluster the broker cluster hosting the topic
    * @param topicName topic name which used to filter the correct meter
    * @return meters belong to the input topic
    */
  private[this] def metrics(brokerCluster: BrokerClusterInfo, topicName: String)(
    implicit meterCache: MeterCache): Metrics = Metrics(
    meterCache.meters(brokerCluster).getOrElse(topicName, Seq.empty))

  /**
    * update the metrics for input topic
    * @param brokerCluster the broker cluster hosting the topic
    * @param topicInfo topic info
    * @return updated topic info
    */
  private[this] def update(brokerCluster: BrokerClusterInfo, topicInfo: TopicInfo)(
    implicit meterCache: MeterCache): TopicInfo = topicInfo.copy(
    metrics = metrics(brokerCluster, topicInfo.name)
  )

  private[this] def createTopic(
    client: TopicAdmin,
    clusterName: String,
    name: String,
    numberOfPartitions: Int,
    numberOfReplications: Short,
    tags: Map[String, JsValue])(implicit executionContext: ExecutionContext): Future[TopicInfo] =
    client.creator
      .name(name)
      .numberOfPartitions(numberOfPartitions)
      .numberOfReplications(numberOfReplications)
      .threadPool(executionContext)
      .create()
      .map { info =>
        try TopicInfo(
          name,
          info.numberOfPartitions,
          info.numberOfReplications,
          clusterName,
          // the topic is just created so we don't fetch the "empty" metrics actually.
          metrics = Metrics(Seq.empty),
          CommonUtils.current(),
          tags
        )
        finally client.close()
      }

  private[this] def hookOfGet(implicit meterCache: MeterCache,
                              brokerCollie: BrokerCollie,
                              executionContext: ExecutionContext): HookOfGet[TopicInfo] = (topicInfo: TopicInfo) =>
    brokerCollie.cluster(topicInfo.brokerClusterName).map {
      case (cluster, _) => update(cluster, topicInfo)
  }

  private[this] def hookOfList(implicit meterCache: MeterCache,
                               brokerCollie: BrokerCollie,
                               executionContext: ExecutionContext): HookOfList[TopicInfo] =
    (topicInfos: Seq[TopicInfo]) =>
      Future.traverse(topicInfos) { response =>
        brokerCollie.cluster(response.brokerClusterName).map {
          case (cluster, _) => update(cluster, response)
        }
    }

  private[this] def hookOfCreation(implicit adminCleaner: AdminCleaner,
                                   brokerCollie: BrokerCollie,
                                   executionContext: ExecutionContext): HookOfCreation[Creation, TopicInfo] =
    (_: String, creation: Creation) =>
      CollieUtils.topicAdmin(creation.brokerClusterName).flatMap {
        case (cluster, client) =>
          client.list().map(_.find(_.name == creation.name)).flatMap { previous =>
            if (previous.isDefined) Future.failed(new IllegalArgumentException(s"${creation.name} already exists"))
            else
              createTopic(
                client = client,
                clusterName = cluster.name,
                name = creation.name,
                numberOfPartitions = creation.numberOfPartitions,
                numberOfReplications = creation.numberOfReplications,
                tags = creation.tags
              )
          }
    }

  private[this] def hookOfUpdate(implicit adminCleaner: AdminCleaner,
                                 brokerCollie: BrokerCollie,
                                 executionContext: ExecutionContext): HookOfUpdate[Creation, Update, TopicInfo] =
    (key: DataKey, update: Update, previous: Option[TopicInfo]) =>
      if (previous.map(_.brokerClusterName).exists(bkName => update.brokerClusterName.exists(_ != bkName)))
        Future.failed(new IllegalArgumentException("It is illegal to move topic to another broker cluster"))
      else
        CollieUtils.topicAdmin(previous.map(_.brokerClusterName).orElse(update.brokerClusterName)).flatMap {
          case (cluster, client) =>
            client.list.map(_.find(_.name == key.name)).flatMap { topicFromKafkaOption =>
              topicFromKafkaOption.fold(createTopic(
                client = client,
                clusterName = cluster.name,
                name = key.name,
                numberOfPartitions = update.numberOfPartitions.getOrElse(DEFAULT_NUMBER_OF_PARTITIONS),
                numberOfReplications = update.numberOfReplications.getOrElse(DEFAULT_NUMBER_OF_REPLICATIONS),
                tags = update.tags.getOrElse(Map.empty)
              )) { topicFromKafka =>
                if (update.numberOfPartitions.exists(_ < topicFromKafka.numberOfPartitions)) {
                  Releasable.close(client)
                  Future.failed(new IllegalArgumentException("Reducing the number from partitions is disallowed"))
                } else if (update.numberOfReplications.exists(_ != topicFromKafka.numberOfReplications)) {
                  // we have got to release the client
                  Releasable.close(client)
                  Future.failed(new IllegalArgumentException("Non-support to change the number from replications"))
                } else if (update.numberOfPartitions.exists(_ > topicFromKafka.numberOfPartitions)) {
                  client.changePartitions(key.name, update.numberOfPartitions.get).map { info =>
                    try TopicInfo(
                      info.name,
                      info.numberOfPartitions,
                      info.numberOfReplications,
                      cluster.name,
                      metrics = Metrics(Seq.empty),
                      CommonUtils.current(),
                      // the topic exists so previous must be defined
                      tags = update.tags.getOrElse(previous.get.tags)
                    )
                    finally client.close()
                  }
                } else {
                  // we have got to release the client
                  Releasable.close(client)
                  // just return the topic info
                  Future.successful(TopicInfo(
                    topicFromKafka.name,
                    topicFromKafka.numberOfPartitions,
                    topicFromKafka.numberOfReplications,
                    cluster.name,
                    metrics = Metrics(Seq.empty),
                    CommonUtils.current(),
                    tags = update.tags.getOrElse(previous.map(_.tags).getOrElse(Map.empty))
                  ))
                }
              }
            }
      }

  private[this] def hookBeforeDelete(implicit store: DataStore,
                                     adminCleaner: AdminCleaner,
                                     brokerCollie: BrokerCollie,
                                     executionContext: ExecutionContext): HookBeforeDelete = (key: DataKey) =>
    store
      .get[TopicInfo](key)
      .flatMap(_.fold(Future.unit) { topicInfo =>
        CollieUtils
          .topicAdmin(Some(topicInfo.brokerClusterName))
          .flatMap {
            case (_, client) =>
              client
                .delete(topicInfo.name)
                .flatMap { _ =>
                  try Future.unit
                  finally Releasable.close(client)
                }
                .recoverWith {
                  case e: Throwable =>
                    LOG.error(s"failed to remove topic:${topicInfo.name} from kafka", e)
                    Future.unit
                }
          }
          .recoverWith {
            case e: NoSuchClusterException =>
              LOG.warn(
                s"the cluster:${topicInfo.brokerClusterName} doesn't exist!!! just remove topic from configurator",
                e)
              Future.unit
          }
      })

  def apply(implicit store: DataStore,
            adminCleaner: AdminCleaner,
            meterCache: MeterCache,
            brokerCollie: BrokerCollie,
            executionContext: ExecutionContext): server.Route =
    RouteUtils.route[Creation, Update, TopicInfo](
      root = TOPICS_PREFIX_PATH,
      enableGroup = false,
      hookOfCreation = hookOfCreation,
      hookOfUpdate = hookOfUpdate,
      hookOfGet = hookOfGet,
      hookOfList = hookOfList,
      hookBeforeDelete = hookBeforeDelete
    )
}
