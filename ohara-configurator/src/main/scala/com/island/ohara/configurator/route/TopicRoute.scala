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
import com.island.ohara.agent.BrokerCollie
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorInfo
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.client.configurator.v0.TopicApi._
import com.island.ohara.common.setting.{ObjectKey, TopicKey}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.route.ObjectChecker.Condition.{RUNNING, STOPPED}
import com.island.ohara.configurator.route.ObjectChecker.ObjectCheckException
import com.island.ohara.configurator.route.hook._
import com.island.ohara.configurator.store.{DataStore, MeterCache}
import com.typesafe.scalalogging.Logger
import spray.json.JsString

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
    * @param topicInfo topic info
    * @return updated topic info
    */
  private[this] def updateState(topicInfo: TopicInfo)(implicit meterCache: MeterCache,
                                                      adminCleaner: AdminCleaner,
                                                      objectChecker: ObjectChecker,
                                                      brokerCollie: BrokerCollie,
                                                      executionContext: ExecutionContext): Future[TopicInfo] =
    objectChecker.checkList
      .topic(topicInfo.key)
      .check()
      .map(_.topicInfos.head._2)
      .flatMap {
        case STOPPED =>
          Future.successful(
            topicInfo.copy(
              partitionInfos = Seq.empty,
              metrics = Metrics.EMPTY,
              state = None
            ))
        case RUNNING =>
          objectChecker.checkList
            .brokerCluster(topicInfo.brokerClusterKey, RUNNING)
            .check()
            .map(_.runningBrokers.head)
            .flatMap { brokerClusterInfo =>
              topicAdmin(brokerClusterInfo).flatMap { topicAdmin =>
                topicAdmin
                  .exist(topicInfo.key)
                  .flatMap(
                    if (_)
                      topicAdmin
                        .topics()
                        .map(_.find(_.name == topicInfo.key.topicNameOnKafka()).get)
                        .map(_.partitionInfos -> Some(TopicState.RUNNING))
                    else Future.successful(Seq.empty -> None))
                  // pre-close topic admin
                  .map(try _
                  finally Releasable.close(topicAdmin))
                  .map {
                    case (partitions, state) =>
                      topicInfo.copy(
                        partitionInfos = partitions.map(partition =>
                          PartitionInfo(
                            index = partition.index,
                            leaderNode = partition.leaderNode,
                            replicaNodes = partition.replicaNodes,
                            inSyncReplicaNodes = partition.inSyncReplicaNodes,
                            beginningOffset = partition.beginningOffset,
                            endOffset = partition.endOffset
                        )),
                        state = state,
                        metrics = metrics(brokerClusterInfo, topicInfo.key.topicNameOnKafka)
                      )
                  }
              }
            }
      }
      .recover {
        case e: Throwable =>
          LOG.debug(s"failed to fetch stats for $topicInfo", e)
          topicInfo.copy(
            partitionInfos = Seq.empty,
            metrics = Metrics.EMPTY,
            state = None
          )
      }

  private[this] def hookOfGet(implicit meterCache: MeterCache,
                              adminCleaner: AdminCleaner,
                              objectChecker: ObjectChecker,
                              brokerCollie: BrokerCollie,
                              executionContext: ExecutionContext): HookOfGet[TopicInfo] = (topicInfo: TopicInfo) =>
    updateState(topicInfo)

  private[this] def hookOfList(implicit meterCache: MeterCache,
                               adminCleaner: AdminCleaner,
                               objectChecker: ObjectChecker,
                               brokerCollie: BrokerCollie,
                               executionContext: ExecutionContext): HookOfList[TopicInfo] =
    (topicInfos: Seq[TopicInfo]) => Future.traverse(topicInfos)(updateState)

  private[this] def creationToTopicInfo(creation: Creation)(implicit objectChecker: ObjectChecker,
                                                            executionContext: ExecutionContext): Future[TopicInfo] =
    objectChecker.checkList.brokerCluster(creation.brokerClusterKey).check().map { _ =>
      TopicInfo(
        settings = creation.settings,
        partitionInfos = Seq.empty,
        metrics = Metrics.EMPTY,
        state = None,
        lastModified = CommonUtils.current()
      )
    }

  private[this] def hookOfCreation(implicit objectChecker: ObjectChecker,
                                   executionContext: ExecutionContext): HookOfCreation[Creation, TopicInfo] =
    creationToTopicInfo(_)

  private[this] def hookOfUpdating(implicit objectChecker: ObjectChecker,
                                   executionContext: ExecutionContext): HookOfUpdating[Updating, TopicInfo] =
    (key: ObjectKey, updating: Updating, previousOption: Option[TopicInfo]) =>
      previousOption match {
        case None =>
          creationToTopicInfo(
            access.request
              .settings(updating.settings)
              // the key is not in update's settings so we have to add it to settings
              .name(key.name)
              .group(key.group)
              .creation)
        case Some(previous) =>
          objectChecker.checkList
          // we don't support to update a running topic
            .topic(previous.key, STOPPED)
            .check()
            .flatMap { _ =>
              // 1) fill the previous settings (if exists)
              // 2) overwrite previous settings by updated settings
              // 3) fill the ignored settings by creation
              creationToTopicInfo(
                access.request
                  .settings(previous.settings)
                  .settings(updating.settings)
                  // the key is not in update's settings so we have to add it to settings
                  .name(key.name)
                  .group(key.group)
                  .creation)
            }
    }

  private[this] def checkConflict(topicInfo: TopicInfo,
                                  connectorInfos: Seq[ConnectorInfo],
                                  streamClusterInfos: Seq[StreamClusterInfo]): Unit = {
    val conflictConnectors = connectorInfos.filter(_.topicKeys.contains(topicInfo.key))
    if (conflictConnectors.nonEmpty)
      throw new IllegalArgumentException(
        s"topic:${topicInfo.key} is used by running connectors:${conflictConnectors.map(_.key).mkString(",")}")
    val conflictStreamApps =
      streamClusterInfos.filter(s => s.fromTopicKeys.contains(topicInfo.key) || s.toTopicKeys.contains(topicInfo.key))
    if (conflictStreamApps.nonEmpty)
      throw new IllegalArgumentException(
        s"topic:${topicInfo.key} is used by running streamApps:${conflictStreamApps.map(_.key).mkString(",")}")
  }

  private[this] def hookBeforeDelete(implicit objectChecker: ObjectChecker,
                                     executionContext: ExecutionContext): HookBeforeDelete = (key: ObjectKey) =>
    objectChecker.checkList
      .topic(TopicKey.of(key.group(), key.name()), STOPPED)
      .allConnectors()
      .allStreamApps()
      .check()
      .map { report =>
        checkConflict(report.topicInfos.head._1, report.connectorInfos.keys.toSeq, report.streamClusterInfos.keys.toSeq)
        Unit
      }
      .recover {
        // the duplicate deletes are legal to ohara
        case e: ObjectCheckException if e.nonexistent.contains(key) => Unit
        case e: Throwable                                           => throw e
      }
      .map(_ => Unit)

  private[this] def hookOfStart(implicit objectChecker: ObjectChecker,
                                adminCleaner: AdminCleaner,
                                brokerCollie: BrokerCollie,
                                executionContext: ExecutionContext): HookOfAction[TopicInfo] =
    (topicInfo: TopicInfo, _, _) =>
      objectChecker.checkList
        .topic(topicInfo.key)
        .brokerCluster(topicInfo.brokerClusterKey, RUNNING)
        .check()
        .map(report => (report.topicInfos.head._2, report.runningBrokers.head))
        .flatMap {
          case (condition, brokerClusterInfo) =>
            condition match {
              case RUNNING => Future.unit
              case STOPPED =>
                topicAdmin(brokerClusterInfo).flatMap { topicAdmin =>
                  topicAdmin.creator
                    .topicKey(topicInfo.key)
                    .numberOfPartitions(topicInfo.numberOfPartitions)
                    .numberOfReplications(topicInfo.numberOfReplications)
                    .configs(topicInfo.configs.map {
                      case (key, value) =>
                        key -> (value match {
                          case JsString(value) => value
                          case _               => value.toString()
                        })
                    })
                    .create()
                    .flatMap(_ =>
                      try Future.unit
                      finally Releasable.close(topicAdmin))
                }
            }
      }

  private[this] def hookOfStop(implicit objectChecker: ObjectChecker,
                               adminCleaner: AdminCleaner,
                               brokerCollie: BrokerCollie,
                               executionContext: ExecutionContext): HookOfAction[TopicInfo] =
    (topicInfo: TopicInfo, _, _) =>
      objectChecker.checkList
        .allConnectors()
        .allStreamApps()
        .topic(topicInfo.key)
        .check()
        .map(report => (report.topicInfos.head._2, report.runningConnectors, report.runningStreamApps))
        .flatMap {
          case (condition, runningConnectors, runningStreamApps) =>
            condition match {
              case STOPPED => Future.unit
              case RUNNING =>
                checkConflict(topicInfo, runningConnectors, runningStreamApps)
                objectChecker.checkList
                // topic is running so the related broker MUST be running
                  .brokerCluster(topicInfo.brokerClusterKey, RUNNING)
                  .check()
                  .map(_.runningBrokers.head)
                  .flatMap(topicAdmin)
                  .flatMap { topicAdmin =>
                    topicAdmin
                      .delete(topicInfo.key)
                      .flatMap(_ =>
                        try Future.unit
                        finally Releasable.close(topicAdmin))
                  }
            }
      }

  def apply(implicit store: DataStore,
            objectChecker: ObjectChecker,
            adminCleaner: AdminCleaner,
            meterCache: MeterCache,
            brokerCollie: BrokerCollie,
            executionContext: ExecutionContext): server.Route =
    RouteBuilder[Creation, Updating, TopicInfo]()
      .root(TOPICS_PREFIX_PATH)
      .hookOfCreation(hookOfCreation)
      .hookOfUpdating(hookOfUpdating)
      .hookOfGet(hookOfGet)
      .hookOfList(hookOfList)
      .hookBeforeDelete(hookBeforeDelete)
      .hookOfPutAction(START_COMMAND, hookOfStart)
      .hookOfPutAction(STOP_COMMAND, hookOfStop)
      .build()
}
