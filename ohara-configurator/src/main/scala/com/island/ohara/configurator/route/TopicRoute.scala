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
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.TopicApi._
import com.island.ohara.client.kafka.TopicAdmin
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.route.RouteUtils._
import com.island.ohara.configurator.store.{DataStore, MeterCache}
import com.island.ohara.kafka.connector.json.{ObjectKey, TopicKey}
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
    metrics = metrics(brokerCluster, topicInfo.key.topicNameOnKafka)
  )

  private[this] def createTopic(topicAdmin: TopicAdmin, topicInfo: TopicInfo)(
    implicit executionContext: ExecutionContext): Future[TopicInfo] =
    topicAdmin.creator
      .name(topicInfo.topicNameOnKafka)
      .numberOfPartitions(topicInfo.numberOfPartitions)
      .numberOfReplications(topicInfo.numberOfReplications)
      .configs(topicInfo.configs)
      .create()
      .map { _ =>
        try topicInfo.copy(
          // the topic is just created so we don't fetch the "empty" metrics actually.
          metrics = Metrics(Seq.empty),
          state = Some(TopicState.RUNNING),
          lastModified = CommonUtils.current(),
        )
        finally topicAdmin.close()
      }

  private[this] def createTopic(
    topicAdmin: TopicAdmin,
    clusterName: String,
    group: String,
    name: String,
    numberOfPartitions: Int,
    numberOfReplications: Short,
    configs: Map[String, String],
    tags: Map[String, JsValue])(implicit executionContext: ExecutionContext): Future[TopicInfo] =
    createTopic(
      topicAdmin = topicAdmin,
      topicInfo = TopicInfo(
        group = group,
        name = name,
        numberOfPartitions = numberOfPartitions,
        numberOfReplications = numberOfReplications,
        brokerClusterName = clusterName,
        metrics = Metrics(Seq.empty),
        state = Some(TopicState.RUNNING),
        lastModified = CommonUtils.current(),
        configs = configs,
        tags = tags
      )
    )

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
    (creation: Creation) =>
      CollieUtils.topicAdmin(creation.brokerClusterName).flatMap {
        case (cluster, client) =>
          // TODO: remove this deprecated behavior. We pre-create the topic on kafka only if the input arguments is completed
          // This stuff is for backward-compatibility.
          if (creation.brokerClusterName.isDefined)
            client.topics().map(_.find(_.name == creation.key.topicNameOnKafka)).flatMap { previous =>
              if (previous.isDefined) Future.failed(new IllegalArgumentException(s"${creation.name} already exists"))
              else
                createTopic(
                  topicAdmin = client,
                  clusterName = cluster.name,
                  group = creation.group,
                  name = creation.name,
                  numberOfPartitions = creation.numberOfPartitions,
                  numberOfReplications = creation.numberOfReplications,
                  configs = creation.configs,
                  tags = creation.tags
                )
            } else
            Future.successful(
              TopicInfo(
                group = creation.group,
                name = creation.name,
                numberOfPartitions = creation.numberOfPartitions,
                numberOfReplications = creation.numberOfReplications,
                brokerClusterName = cluster.name,
                metrics = Metrics(Seq.empty),
                state = None,
                lastModified = CommonUtils.current(),
                configs = creation.configs,
                tags = creation.tags
              ))
    }

  private[this] def hookOfUpdate(implicit adminCleaner: AdminCleaner,
                                 brokerCollie: BrokerCollie,
                                 executionContext: ExecutionContext): HookOfUpdate[Creation, Update, TopicInfo] =
    (key: ObjectKey, update: Update, previous: Option[TopicInfo]) =>
      if (previous.map(_.brokerClusterName).exists(bkName => update.brokerClusterName.exists(_ != bkName)))
        Future.failed(new IllegalArgumentException("It is illegal to move topic to another broker cluster"))
      else
        CollieUtils.topicAdmin(previous.map(_.brokerClusterName).orElse(update.brokerClusterName)).flatMap {
          case (cluster, client) =>
            client.topics().map(_.find(_.name == TopicKey.of(key.group, key.name).topicNameOnKafka)).flatMap {
              topicFromKafkaOption =>
                topicFromKafkaOption.fold(Future.successful(TopicInfo(
                  group = key.group,
                  name = key.name,
                  numberOfPartitions = update.numberOfPartitions.getOrElse(DEFAULT_NUMBER_OF_PARTITIONS),
                  numberOfReplications = update.numberOfReplications.getOrElse(DEFAULT_NUMBER_OF_REPLICATIONS),
                  brokerClusterName = cluster.name,
                  metrics = Metrics(Seq.empty),
                  state = None,
                  lastModified = CommonUtils.current(),
                  configs = update.configs.orElse(previous.map(_.configs)).getOrElse(Map.empty),
                  tags = update.tags.orElse(previous.map(_.tags)).getOrElse(Map.empty)
                ))) { topicFromKafka =>
                  if (update.configs.exists(_ != topicFromKafka.configs)) {
                    Releasable.close(client)
                    Future.failed(new IllegalArgumentException("Changing configs is disallowed"))
                  } else if (update.numberOfPartitions.exists(_ < topicFromKafka.numberOfPartitions)) {
                    Releasable.close(client)
                    Future.failed(new IllegalArgumentException("Reducing the number from partitions is disallowed"))
                  } else if (update.numberOfReplications.exists(_ != topicFromKafka.numberOfReplications)) {
                    // we have got to release the client
                    Releasable.close(client)
                    Future.failed(new IllegalArgumentException("Non-support to change the number from replications"))
                  } else if (update.numberOfPartitions.exists(_ > topicFromKafka.numberOfPartitions)) {
                    client.changePartitions(key.name, update.numberOfPartitions.get).map { _ =>
                      try TopicInfo(
                        group = key.group,
                        name = key.name,
                        numberOfPartitions = update.numberOfPartitions.get,
                        numberOfReplications = topicFromKafka.numberOfReplications,
                        brokerClusterName = cluster.name,
                        metrics = Metrics(Seq.empty),
                        state = Some(TopicState.RUNNING),
                        lastModified = CommonUtils.current(),
                        configs = topicFromKafka.configs,
                        tags = update.tags.orElse(previous.map(_.tags)).getOrElse(Map.empty)
                      )
                      finally client.close()
                    }
                  } else {
                    // we have got to release the client
                    Releasable.close(client)
                    // just return the topic info
                    Future.successful(TopicInfo(
                      group = key.group,
                      name = key.name,
                      numberOfPartitions = topicFromKafka.numberOfPartitions,
                      numberOfReplications = topicFromKafka.numberOfReplications,
                      brokerClusterName = cluster.name,
                      metrics = Metrics(Seq.empty),
                      state = Some(TopicState.RUNNING),
                      lastModified = CommonUtils.current(),
                      configs = topicFromKafka.configs,
                      tags = update.tags.orElse(previous.map(_.tags)).getOrElse(Map.empty)
                    ))
                  }
                }
            }
      }

  private[this] def hookBeforeDelete(implicit store: DataStore,
                                     adminCleaner: AdminCleaner,
                                     brokerCollie: BrokerCollie,
                                     executionContext: ExecutionContext): HookBeforeDelete = (key: ObjectKey) =>
    store
      .get[TopicInfo](key)
      .flatMap(_.fold(Future.unit) { topicInfo =>
        CollieUtils
          .topicAdmin(Some(topicInfo.brokerClusterName))
          .flatMap {
            case (_, client) =>
              client
                .exist(topicInfo.name)
                .flatMap {
                  // TODO: it should forbid user to delete a running topic... by chia
                  if (_) client.delete(topicInfo.name).flatMap { _ =>
                    try Future.unit
                    finally Releasable.close(client)
                  } else
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

  private[this] def hookOfStart(implicit store: DataStore,
                                adminCleaner: AdminCleaner,
                                brokerCollie: BrokerCollie,
                                executionContext: ExecutionContext): HookOfStart[TopicInfo] =
    (key: ObjectKey) =>
      store
        .value[TopicInfo](key)
        .flatMap(topicInfo =>
          CollieUtils.topicAdmin(Some(topicInfo.brokerClusterName)).flatMap {
            case (_, client) =>
              client.exist(topicInfo.name).flatMap {
                if (_) Future.successful(topicInfo.copy(state = Some(TopicState.RUNNING)))
                else
                  createTopic(
                    topicAdmin = client,
                    topicInfo = topicInfo
                  )
              }
        })

  private[this] def hookOfStop(implicit store: DataStore,
                               adminCleaner: AdminCleaner,
                               brokerCollie: BrokerCollie,
                               executionContext: ExecutionContext): HookOfStop[TopicInfo] =
    (key: ObjectKey) =>
      store.value[TopicInfo](key).flatMap { topicInfo =>
        CollieUtils.topicAdmin(Some(topicInfo.brokerClusterName)).flatMap {
          case (_, client) =>
            client
              .exist(topicInfo.name)
              .flatMap {
                if (_) client.delete(topicInfo.name)
                else Future.unit
              }
              .map(
                _ =>
                  topicInfo.copy(
                    state = None,
                    lastModified = CommonUtils.current()
                ))
        }
    }

  private[this] def hookOfGroup: HookOfGroup = _.getOrElse(GROUP_DEFAULT)

  def apply(implicit store: DataStore,
            adminCleaner: AdminCleaner,
            meterCache: MeterCache,
            brokerCollie: BrokerCollie,
            executionContext: ExecutionContext): server.Route =
    RouteUtils.route[Creation, Update, TopicInfo](
      root = TOPICS_PREFIX_PATH,
      hookOfGroup = hookOfGroup,
      hookOfCreation = hookOfCreation,
      hookOfUpdate = hookOfUpdate,
      hookOfGet = hookOfGet,
      hookOfList = hookOfList,
      hookBeforeDelete = hookBeforeDelete,
      hookOfStart = hookOfStart,
      hookOfStop = hookOfStop
    )
}
