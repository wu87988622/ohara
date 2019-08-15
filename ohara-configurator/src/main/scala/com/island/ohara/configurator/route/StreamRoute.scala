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

import java.util.Objects

import akka.http.scaladsl.server
import com.island.ohara.agent._
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.StreamApi._
import com.island.ohara.client.kafka.TopicAdmin.TopicInfo
import com.island.ohara.common.setting.{ObjectKey, TopicKey}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.file.FileStore
import com.island.ohara.configurator.route.hook.{HookOfAction, HookOfCreation, HookOfGroup, HookOfUpdate}
import com.island.ohara.configurator.store.{DataStore, MeterCache}
import com.island.ohara.streams.config.StreamDefinitions
import org.slf4j.LoggerFactory
import spray.json._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
private[configurator] object StreamRoute {

  /**
    * The group for a stream application metrics
    * Since each streamApp has it's own metrics, it is OK to use same value
    */
  private[configurator] val STREAM_APP_GROUP = StreamDefinitions.STREAMAPP_METRIC_GROUP_DEFINITION.defaultValue()
  private[this] val log = LoggerFactory.getLogger(StreamRoute.getClass)

  /**
    * Save the streamApp properties.
    * This method will try to fetch the definitions of custom jar.
    * Note: request fields must have definition to used in streamApp.
    *
    * @param req the creation request
    * @return '''StreamApp''' object
    */
  private[this] def toStore(req: Creation)(implicit
                                           fileStore: FileStore,
                                           clusterCollie: ClusterCollie,
                                           executionContext: ExecutionContext): Future[StreamClusterInfo] = {
    req.jarKey.fold {
      log.info(s"there is no jar provided, we skip definition...")
      Future.successful(
        StreamClusterInfo(
          settings = req.settings,
          definition = None,
          nodeNames = req.nodeNames,
          deadNodes = Set.empty,
          state = None,
          metrics = Metrics(Seq.empty),
          error = None,
          lastModified = CommonUtils.current()
        ))
    } { jarKey =>
      fileStore
        .fileInfo(jarKey)
        .map(_.url)
        .flatMap(
          url =>
            clusterCollie.streamCollie
              .loadDefinition(url)
              .map(streamDefOption =>
                StreamClusterInfo(
                  settings = req.settings,
                  definition = streamDefOption,
                  nodeNames = req.nodeNames,
                  deadNodes = Set.empty,
                  state = None,
                  metrics = Metrics(Seq.empty),
                  error = None,
                  lastModified = CommonUtils.current()
              )))
    }
  }

  /**
    * Assert the require streamApp properties before running
    *
    * @param data streamApp data
    */
  private[this] def assertParameters(data: StreamClusterInfo, topicInfos: Seq[TopicInfo]): Unit = {
    CommonUtils.requireNonEmpty(data.name, () => "name fail assert")
    CommonUtils.requireConnectionPort(data.jmxPort)
    Objects.requireNonNull(data.jarKey)

    def checkStoppedTopics(topicKeys: Set[TopicKey], prefix: String): Unit = {
      CommonUtils.requireNonEmpty(topicKeys.asJava, () => s"$prefix topics can't be empty")
      // check the from/to topic size equals one
      // TODO: this is a workaround to avoid input multiple topics
      // TODO: please refactor this after the single from/to topic issue resolved...by Sam
      if (topicKeys.size > 1)
        throw new IllegalArgumentException(
          s"We don't allow multiple topics of $prefix field, actual: ${topicKeys.mkString(",")}")
      val stoppedFromTopics = topicKeys.filterNot(topicKey => topicInfos.exists(_.name == topicKey.topicNameOnKafka()))
      if (stoppedFromTopics.nonEmpty)
        throw new NoSuchElementException(s"topics:${stoppedFromTopics.mkString(",")} is not running")
    }
    checkStoppedTopics(data.from, "from")
    checkStoppedTopics(data.to, "to")
  }

  private[this] def hookOfCreation(implicit fileStore: FileStore,
                                   clusterCollie: ClusterCollie,
                                   executionContext: ExecutionContext): HookOfCreation[Creation, StreamClusterInfo] =
    (creation: Creation) => toStore(creation)

  private[this] def hookOfUpdate: HookOfUpdate[Creation, Update, StreamClusterInfo] =
    (key: ObjectKey, req: Update, previousOption: Option[StreamClusterInfo]) => {
      val updateReq = previousOption.fold(
        // data not exists, we used PUT as create object method
        StreamClusterInfo(
          settings = req.settings,
          definition = None,
          nodeNames = req.nodeNames.getOrElse(Set.empty),
          deadNodes = Set.empty,
          state = None,
          metrics = Metrics(Seq.empty),
          error = None,
          lastModified = CommonUtils.current()
        )
      ) { previous =>
        previous.copy(
          settings = previous.settings ++ req.settings,
          nodeNames = req.nodeNames.getOrElse(previous.nodeNames)
        )
      }
      if (updateReq.state.isDefined)
        throw new RuntimeException(
          s"You cannot update property on non-stopped streamApp: $key"
        )
      else Future.successful(updateReq)
    }

  private[this] def hookOfStart(implicit store: DataStore,
                                fileStore: FileStore,
                                adminCleaner: AdminCleaner,
                                nodeCollie: NodeCollie,
                                clusterCollie: ClusterCollie,
                                workerCollie: WorkerCollie,
                                brokerCollie: BrokerCollie,
                                executionContext: ExecutionContext): HookOfAction =
    (key: ObjectKey, _, _) =>
      store.value[StreamClusterInfo](key).flatMap { data =>
        // we assume streamApp has following conditions:
        // 1) use any available node of worker cluster to run streamApp
        // 2) use one from/to pair topic (multiple from/to topics will need to discuss flow)
        // get the broker info and topic info from worker cluster name
        // TODO: decouple this cryptic dependency ... by chia (see https://github.com/oharastream/ohara/issues/2151)
        CollieUtils
          .both(Some(data.jarKey.group()))
          // get broker props from worker cluster
          .flatMap {
            case (brokerClusterInfo, topicAdmin, _, _) => topicAdmin.topics().map(topics => (brokerClusterInfo, topics))
          }
          .flatMap {
            case (brokerClusterInfo, topicInfos) =>
              fileStore.fileInfo(data.jarKey).flatMap { fileInfo =>
                // check the require fields
                assertParameters(data, topicInfos)
                nodeCollie
                  .nodes()
                  .map { all =>
                    if (CommonUtils.isEmpty(data.nodeNames.asJava)) {
                      // Check instance first
                      // Here we will check the following conditions:
                      // 1. instance should be positive
                      // 2. available nodes should be bigger than instance (one node runs one instance)
                      if (all.size < data.instances)
                        throw new IllegalArgumentException(
                          s"cannot run streamApp. expect: ${data.instances}, actual: ${all.size}")
                      Random.shuffle(all).take(CommonUtils.requirePositiveInt(data.instances)).toSet
                    } else
                      // if require node name is not in nodeCollie, do not take that node
                      CommonUtils.requireNonEmpty(all.filter(n => data.nodeNames.contains(n.name)).asJava).asScala.toSet
                  }
                  .flatMap(nodes => {
                    clusterCollie.streamCollie.creator
                      .clusterName(data.name)
                      .imageName(IMAGE_NAME_DEFAULT)
                      .jarInfo(fileInfo)
                      // these settings will send to container environment
                      // we convert all value to string for convenient
                      .settings(data.settings)
                      .setting(StreamDefinitions.BROKER_DEFINITION.key(), JsString(brokerClusterInfo.connectionProps))
                      // This nodeNames() should put after settings() because we decide nodeName in starting phase
                      // TODO: the order should not be a problem and please refactor this in #2288
                      .nodeNames(nodes.map(_.name))
                      // TODO: we should use boolean type ... by chia
                      .setting(StreamDefinitions.EXACTLY_ONCE_DEFINITION.key(), JsString(data.exactlyOnce.toString))
                      .brokerClusterName(brokerClusterInfo.name)
                      .threadPool(executionContext)
                      .create()
                  })
              }
          }
          .map(_ => Unit)
    }

  private[this] def hookBeforeStop: HookOfAction = (_, _, _) => Future.unit

  private[this] def hookOfGroup: HookOfGroup = _ => GROUP_DEFAULT

  def apply(implicit store: DataStore,
            adminCleaner: AdminCleaner,
            nodeCollie: NodeCollie,
            streamCollie: StreamCollie,
            clusterCollie: ClusterCollie,
            workerCollie: WorkerCollie,
            brokerCollie: BrokerCollie,
            fileStore: FileStore,
            meterCache: MeterCache,
            executionContext: ExecutionContext): server.Route =
    clusterRoute(
      root = STREAM_PREFIX_PATH,
      metricsKey = Some(STREAM_APP_GROUP),
      hookOfGroup = hookOfGroup,
      hookOfCreation = hookOfCreation,
      hookOfUpdate = hookOfUpdate,
      hookOfStart = hookOfStart,
      hookBeforeStop = hookBeforeStop
    )
}
