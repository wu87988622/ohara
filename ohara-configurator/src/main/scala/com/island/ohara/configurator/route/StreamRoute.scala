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
import com.island.ohara.agent._
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.StreamApi._
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.ObjectChecker.Condition.{RUNNING, STOPPED}
import com.island.ohara.configurator.route.hook.{HookBeforeDelete, HookOfAction, HookOfCreation, HookOfUpdating}
import com.island.ohara.configurator.store.{DataStore, MeterCache}
import com.island.ohara.streams.config.StreamDefUtils
import spray.json.DeserializationException

import scala.concurrent.{ExecutionContext, Future}
private[configurator] object StreamRoute {

  /**
    * The group for a stream application metrics
    * Since each streamApp has it's own metrics, it is OK to use same value
    */
  private[configurator] val STREAM_APP_GROUP = StreamDefUtils.STREAM_METRICS_GROUP_DEFAULT

  private[this] def creationToClusterInfo(creation: Creation)(
    implicit objectChecker: ObjectChecker,
    executionContext: ExecutionContext): Future[StreamClusterInfo] =
    objectChecker.checkList
      .nodeNames(creation.nodeNames)
      .file(creation.jarKey)
      .brokerCluster(creation.brokerClusterKey)
      /**
        * TODO: this is a workaround to avoid input multiple topics
        * TODO: please refactor this after the single from/to topic issue resolved...by Sam
        */
      .topics {
        if (creation.fromTopicKeys.size > 1)
          throw new IllegalArgumentException(
            s"the size of from topics MUST be equal to 1 (multiple topics is a unsupported feature)")
        creation.fromTopicKeys
      }
      /**
        * TODO: this is a workaround to avoid input multiple topics
        * TODO: please refactor this after the single from/to topic issue resolved...by Sam
        */
      .topics {
        if (creation.toTopicKeys.size > 1)
          throw new IllegalArgumentException(
            s"the size of from topics MUST be equal to 1 (multiple topics is a unsupported feature)")
        creation.toTopicKeys
      }
      .check()
      .map { _ =>
        StreamClusterInfo(
          settings = creation.settings,
          aliveNodes = Set.empty,
          state = None,
          metrics = Metrics(Seq.empty),
          error = None,
          lastModified = CommonUtils.current()
        )
      }

  private[this] def hookOfCreation(implicit objectChecker: ObjectChecker,
                                   executionContext: ExecutionContext): HookOfCreation[Creation, StreamClusterInfo] =
    creationToClusterInfo(_)

  private[this] def hookOfUpdating(implicit objectChecker: ObjectChecker,
                                   executionContext: ExecutionContext): HookOfUpdating[Updating, StreamClusterInfo] =
    (key: ObjectKey, updating: Updating, previousOption: Option[StreamClusterInfo]) =>
      previousOption match {
        case None =>
          creationToClusterInfo(
            access.request
              .settings(updating.settings)
              // the key is not in update's settings so we have to add it to settings
              .key(key)
              .creation)
        case Some(previous) =>
          objectChecker.checkList
          // we don't support to update a running streamApp
            .streamApp(previous.key, STOPPED)
            .check()
            .flatMap { _ =>
              // 1) fill the previous settings (if exists)
              // 2) overwrite previous settings by updated settings
              // 3) fill the ignored settings by creation
              creationToClusterInfo(
                access.request
                  .settings(previous.settings)
                  .settings(updating.settings)
                  // the key is not in update's settings so we have to add it to settings
                  .key(key)
                  .creation)
            }
    }

  private[this] def hookOfStart(implicit objectChecker: ObjectChecker,
                                streamCollie: StreamCollie,
                                executionContext: ExecutionContext): HookOfAction[StreamClusterInfo] =
    (streamClusterInfo: StreamClusterInfo, _, _) => {
      objectChecker.checkList
      // node names check is covered in super route
        .streamApp(streamClusterInfo.key)
        .file(streamClusterInfo.jarKey)
        .brokerCluster(streamClusterInfo.brokerClusterKey, RUNNING)
        .topics(
          // our UI needs to create a stream without topics so the stream info may has no topics...
          if (streamClusterInfo.toTopicKeys.isEmpty)
            throw DeserializationException(s"${StreamDefUtils.TO_TOPIC_KEYS_DEFINITION.key()} can't be empty",
                                           fieldNames = List(StreamDefUtils.TO_TOPIC_KEYS_DEFINITION.key()))
          else streamClusterInfo.toTopicKeys,
          RUNNING
        )
        .topics(
          // our UI needs to create a stream without topics so the stream info may has no topics...
          if (streamClusterInfo.fromTopicKeys.isEmpty)
            throw DeserializationException(s"${StreamDefUtils.FROM_TOPIC_KEYS_DEFINITION.key()} topics can't be empty",
                                           fieldNames = List(StreamDefUtils.FROM_TOPIC_KEYS_DEFINITION.key()))
          else streamClusterInfo.fromTopicKeys,
          RUNNING
        )
        .check()
        .map(
          report =>
            (report.streamClusterInfos.head._2,
             report.fileInfos.head,
             report.brokerClusterInfos.head._1,
             report.runningTopics))
        .flatMap {
          case (condition, fileInfo, brokerClusterInfo, topicInfos) =>
            condition match {
              case RUNNING => Future.unit
              case STOPPED =>
                topicInfos.filter(_.brokerClusterKey != brokerClusterInfo.key).foreach { topicInfo =>
                  throw new IllegalArgumentException(
                    s"stream app counts on broker cluster:${streamClusterInfo.brokerClusterKey} " +
                      s"but topic:${topicInfo.key} is on another broker cluster:${topicInfo.brokerClusterKey}")
                }
                streamCollie.creator
                // these settings will send to container environment
                // we convert all value to string for convenient
                  .settings(streamClusterInfo.settings)
                  .name(streamClusterInfo.name)
                  .group(streamClusterInfo.group)
                  .nodeNames(streamClusterInfo.nodeNames)
                  .jarKey(fileInfo.key)
                  .brokerClusterKey(brokerClusterInfo.key)
                  .connectionProps(brokerClusterInfo.connectionProps)
                  .threadPool(executionContext)
                  .create()
            }
        }
    }

  private[this] def hookBeforeStop: HookOfAction[StreamClusterInfo] = (_, _, _) => Future.unit

  private[this] def hookBeforeDelete: HookBeforeDelete = _ => Future.unit

  def apply(implicit store: DataStore,
            objectChecker: ObjectChecker,
            streamCollie: StreamCollie,
            serviceCollie: ServiceCollie,
            meterCache: MeterCache,
            executionContext: ExecutionContext): server.Route =
    clusterRoute[StreamClusterInfo, StreamClusterStatus, Creation, Updating](
      root = STREAM_PREFIX_PATH,
      metricsKey = Some(STREAM_APP_GROUP),
      hookOfCreation = hookOfCreation,
      hookOfUpdating = hookOfUpdating,
      hookOfStart = hookOfStart,
      hookBeforeStop = hookBeforeStop,
      hookBeforeDelete = hookBeforeDelete
    )
}
