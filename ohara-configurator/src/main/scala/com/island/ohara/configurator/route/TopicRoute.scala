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
import com.island.ohara.configurator.store.{DataStore, MeterCache}
import com.typesafe.scalalogging.Logger

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
    numberOfReplications: Short)(implicit executionContext: ExecutionContext): Future[TopicInfo] = client
    .creator()
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
        CommonUtils.current()
      )
      finally client.close()
    }

  def apply(implicit store: DataStore,
            adminCleaner: AdminCleaner,
            meterCache: MeterCache,
            brokerCollie: BrokerCollie,
            executionContext: ExecutionContext): server.Route =
    RouteUtils.basicRoute[Creation, Update, TopicInfo](
      root = TOPICS_PREFIX_PATH,
      // we don't care for generated name since topic's name should be equal to the name passed by user.
      hookOfCreation = (creation: Creation) =>
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
                  numberOfReplications = creation.numberOfReplications
                )
            }
      },
      hookOfUpdate = (name: String, update: Update, previous: Option[TopicInfo]) =>
        if (previous.map(_.brokerClusterName).exists(bkName => update.brokerClusterName.exists(_ != bkName)))
          Future.failed(new IllegalArgumentException("It is illegal to move topic to another broker cluster"))
        else
          CollieUtils.topicAdmin(previous.map(_.brokerClusterName).orElse(update.brokerClusterName)).flatMap {
            case (cluster, client) =>
              client.list.map(_.find(_.name == name)).flatMap {
                topicFromKafkaOption =>
                  topicFromKafkaOption
                    .map {
                      topicFromKafka =>
                        if (update.numberOfPartitions.exists(_ < topicFromKafka.numberOfPartitions)) {
                          Releasable.close(client)
                          Future.failed(
                            new IllegalArgumentException("Reducing the number from partitions is disallowed"))
                        } else if (update.numberOfReplications.exists(_ != topicFromKafka.numberOfReplications)) {
                          // we have got to release the client
                          Releasable.close(client)
                          Future.failed(
                            new IllegalArgumentException("Non-support to change the number from replications"))
                        } else if (update.numberOfPartitions.exists(_ > topicFromKafka.numberOfPartitions)) {
                          client.changePartitions(name, update.numberOfPartitions.get).map { info =>
                            try TopicInfo(
                              info.name,
                              info.numberOfPartitions,
                              info.numberOfReplications,
                              cluster.name,
                              metrics = Metrics(Seq.empty),
                              CommonUtils.current()
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
                            CommonUtils.current()
                          ))
                        }
                    }
                    .getOrElse {
                      // topic does not exist so we just create it!
                      createTopic(
                        client = client,
                        clusterName = cluster.name,
                        name = name,
                        numberOfPartitions = update.numberOfPartitions.getOrElse(DEFAULT_NUMBER_OF_PARTITIONS),
                        numberOfReplications = update.numberOfReplications.getOrElse(DEFAULT_NUMBER_OF_REPLICATIONS)
                      )
                    }
              }
        },
      hookBeforeDelete = (name: String) =>
        store
          .get[TopicInfo](name)
          .flatMap(_.map { topicInfo =>
            CollieUtils
              .topicAdmin(Some(topicInfo.brokerClusterName))
              .flatMap {
                case (_, client) =>
                  client
                    .delete(topicInfo.name)
                    .map { _ =>
                      try name
                      finally Releasable.close(client)
                    }
                    .recover {
                      case e: Throwable =>
                        LOG.error(s"failed to remove topic:${topicInfo.name} from kafka", e)
                        name
                    }
              }
              .recover {
                case e: NoSuchClusterException =>
                  LOG.warn(
                    s"the cluster:${topicInfo.brokerClusterName} doesn't exist!!! just remove topic from configurator",
                    e)
                  name
              }
          }.getOrElse(Future.successful(name))),
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
