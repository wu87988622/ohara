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
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.StreamApi._
import com.island.ohara.client.kafka.TopicAdmin.TopicInfo
import com.island.ohara.common.setting.{ObjectKey, TopicKey}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.file.FileStore
import com.island.ohara.configurator.route.hook.{HookOfAction, HookOfCreation, HookOfGroup, HookOfUpdate}
import com.island.ohara.configurator.store.{DataStore, MeterCache}
import com.island.ohara.streams.config.StreamDefUtils
import spray.json._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
private[configurator] object StreamRoute {

  /**
    * The group for a stream application metrics
    * Since each streamApp has it's own metrics, it is OK to use same value
    */
  private[configurator] val STREAM_APP_GROUP = StreamDefUtils.STREAMAPP_METRIC_GROUP_DEFINITION.defaultValue()

  private[this] def assertParameters(streamClusterInfo: StreamClusterInfo): StreamClusterInfo = {
    CommonUtils.requireNonEmpty(streamClusterInfo.name, () => "name fail assert")
    CommonUtils.requireConnectionPort(streamClusterInfo.jmxPort)
    // we don't check the jar key for creation/update since it breaks the APIs.
    // https://github.com/oharastream/ohara/issues/2151
    // Objects.requireNonNull(streamClusterInfo.jarKey)
    streamClusterInfo
  }

  /**
    * Assert the require streamApp properties before running
    *
    * @param streamClusterInfo streamApp data
    */
  private[this] def assertParameters(streamClusterInfo: StreamClusterInfo,
                                     topicInfos: Seq[TopicInfo]): StreamClusterInfo = {
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
    checkStoppedTopics(streamClusterInfo.from, "from")
    checkStoppedTopics(streamClusterInfo.to, "to")
    assertParameters(streamClusterInfo)
  }

  /**
    * fina the broker cluster for this creation. The rules are shown below.
    * 1) fp;nd the defined value for broker cluster name
    * 2) find the group of jar key to seek the worker cluster and its broker cluster
    * (TODO: this is a really really really ugly design and it will be fixed by https://github.com/oharastream/ohara/issues/2151)
    * 2.1) find the single broker cluster if the group of jar key is not associated to any worker cluster
    * 3) find the single broker cluster
    * 4) throw exception
    */
  private[this] def pickBrokerCluster(brokerClusterName: Option[String], jarKey: Option[ObjectKey])(
    implicit workerCollie: WorkerCollie,
    brokerCollie: BrokerCollie,
    executionContext: ExecutionContext): Future[String] = if (brokerClusterName.isDefined)
    Future.successful(brokerClusterName.get)
  else if (jarKey.isDefined) workerCollie.clusters().map(_.keys).flatMap { workerClusters =>
    workerClusters
      .find(_.name == jarKey.get.group())
      .map(_.brokerClusterName)
      .map(Future.successful)
      .getOrElse(CollieUtils.singleCluster[BrokerClusterInfo]())
  } else CollieUtils.singleCluster[BrokerClusterInfo]()

  private[this] def hookOfCreation(implicit fileStore: FileStore,
                                   workerCollie: WorkerCollie,
                                   brokerCollie: BrokerCollie,
                                   streamCollie: StreamCollie,
                                   executionContext: ExecutionContext): HookOfCreation[Creation, StreamClusterInfo] =
    (creation: Creation) =>
      pickBrokerCluster(creation.brokerClusterName, creation.jarKey).flatMap { bkName =>
        creation.jarKey
          .map(fileStore.fileInfo)
          .map(_.map(_.url).flatMap(streamCollie.loadDefinition).map((_, Option.empty[String])))
          .getOrElse(Future.successful((None, None)))
          .recover {
            case e: Throwable => (None, Some(e.getMessage))
          }
          .map {
            case (definition, error) =>
              StreamClusterInfo(
                settings = creation.settings + (StreamDefUtils.BROKER_CLUSTER_NAME_DEFINITION.key() -> JsString(
                  bkName)),
                definition = definition,
                nodeNames = creation.nodeNames,
                deadNodes = Set.empty,
                state = None,
                metrics = Metrics(Seq.empty),
                error = error,
                lastModified = CommonUtils.current()
              )
          }
          .map(assertParameters)
    }

  private[this] def hookOfUpdate(
    implicit workerCollie: WorkerCollie,
    brokerCollie: BrokerCollie,
    streamCollie: StreamCollie,
    executionContext: ExecutionContext): HookOfUpdate[Creation, Update, StreamClusterInfo] =
    (key: ObjectKey, update: Update, previousOption: Option[StreamClusterInfo]) =>
      streamCollie.clusters
        .flatMap { clusters =>
          if (clusters.keys.filter(_.name == key.name()).exists(_.state.nonEmpty))
            throw new RuntimeException(s"You cannot update property on non-stopped StreamApp cluster: $key")
          pickBrokerCluster(update.brokerClusterName.orElse(previousOption.map(_.brokerClusterName)),
                            update.jarKey.orElse(previousOption.map(_.jarKey))).map(bkName =>
            update.copy(
              settings = update.settings + (StreamDefUtils.BROKER_CLUSTER_NAME_DEFINITION.key() -> JsString(bkName))))
        }
        .map { update =>
          StreamClusterInfo(
            settings = previousOption.map(_.settings).getOrElse(Map.empty) ++ update.settings,
            definition = previousOption.flatMap(_.definition),
            nodeNames = update.nodeNames.orElse(previousOption.map(_.nodeNames)).getOrElse(Set.empty),
            // this cluster is not running so we don't need to keep the dead nodes in the updated cluster.
            deadNodes = Set.empty,
            state = None,
            metrics = Metrics(Seq.empty),
            error = None,
            lastModified = CommonUtils.current()
          )
        }
        .map(assertParameters)

  private[this] def hookOfStart(implicit store: DataStore,
                                fileStore: FileStore,
                                nodeCollie: NodeCollie,
                                clusterCollie: ClusterCollie,
                                brokerCollie: BrokerCollie,
                                executionContext: ExecutionContext): HookOfAction =
    (key: ObjectKey, _, _) =>
      store
        .value[StreamClusterInfo](key)
        .flatMap { streamClusterInfo =>
          brokerCollie
            .topicAdmin(streamClusterInfo.brokerClusterName)
            .flatMap {
              case (brokerClusterInfo, topicAdmin) =>
                topicAdmin.topics().map { topicInfos =>
                  try brokerClusterInfo -> topicInfos
                  finally topicAdmin.close()
                }
            }
            .map {
              case (brokerClusterInfo, topicInfos) =>
                assertParameters(streamClusterInfo, topicInfos)
                brokerClusterInfo
            }
            .flatMap { brokerClusterInfo =>
              fileStore.fileInfo(streamClusterInfo.jarKey).flatMap { fileInfo =>
                nodeCollie
                  .nodes()
                  .map { all =>
                    if (CommonUtils.isEmpty(streamClusterInfo.nodeNames.asJava)) {
                      // Check instance first
                      // Here we will check the following conditions:
                      // 1. instance should be positive
                      // 2. available nodes should be bigger than instance (one node runs one instance)
                      if (all.size < streamClusterInfo.instances)
                        throw new IllegalArgumentException(
                          s"cannot run streamApp. expect: ${streamClusterInfo.instances}, actual: ${all.size}")
                      Random.shuffle(all).take(CommonUtils.requirePositiveInt(streamClusterInfo.instances)).toSet
                    } else
                      // if require node name is not in nodeCollie, do not take that node
                      CommonUtils
                        .requireNonEmpty(all.filter(n => streamClusterInfo.nodeNames.contains(n.name)).asJava)
                        .asScala
                        .toSet
                  }
                  .flatMap { nodes =>
                    clusterCollie.streamCollie.creator
                      .clusterName(streamClusterInfo.name)
                      .imageName(IMAGE_NAME_DEFAULT)
                      .jarInfo(fileInfo)
                      // these settings will send to container environment
                      // we convert all value to string for convenient
                      .settings(streamClusterInfo.settings)
                      .setting(StreamDefUtils.BROKER_DEFINITION.key(), JsString(brokerClusterInfo.connectionProps))
                      // This nodeNames() should put after settings() because we decide nodeName in starting phase
                      // TODO: the order should not be a problem and please refactor this in #2288
                      .nodeNames(nodes.map(_.name))
                      .setting(StreamDefUtils.EXACTLY_ONCE_DEFINITION.key(),
                               JsString(streamClusterInfo.exactlyOnce.toString))
                      .setting(StreamDefUtils.EXACTLY_ONCE_DEFINITION.key(),
                               JsString(streamClusterInfo.exactlyOnce.toString))
                      .brokerClusterName(brokerClusterInfo.name)
                      .threadPool(executionContext)
                      .create()
                  }
              }
            }
        }
        .map(_ => Unit)

  private[this] def hookBeforeStop: HookOfAction = (_, _, _) => Future.unit

  private[this] def hookOfGroup: HookOfGroup = _ => GROUP_DEFAULT

  def apply(implicit store: DataStore,
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
