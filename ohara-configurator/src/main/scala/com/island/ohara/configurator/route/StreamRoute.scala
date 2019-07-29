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
import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.agent.{BrokerCollie, ClusterCollie, NodeCollie, WorkerCollie}
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.StreamApi._
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.file.FileStore
import com.island.ohara.configurator.route.RouteUtils.{START_COMMAND => _, STOP_COMMAND => _, _}
import com.island.ohara.configurator.store.{DataStore, MeterCache}
import com.island.ohara.kafka.connector.json.ObjectKey
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

private[configurator] object StreamRoute {

  private[this] val log = LoggerFactory.getLogger(StreamRoute.getClass)

  /**
    * save the streamApp properties
    *
    * @param creation the creation request
    * @return '''StreamApp''' object
    */
  private[this] def toStreamAppDescription(creation: Creation): StreamAppDescription =
    StreamAppDescription(
      name = creation.name,
      imageName = creation.imageName,
      instances = if (creation.nodeNames.isEmpty) creation.instances else creation.nodeNames.size,
      nodeNames = creation.nodeNames,
      deadNodes = Set.empty,
      jar = creation.jar,
      from = creation.from,
      to = creation.to,
      state = None,
      jmxPort = creation.jmxPort,
      metrics = Metrics(Seq.empty),
      error = None,
      lastModified = CommonUtils.current(),
      tags = creation.tags
    )

  /**
    * Update the streamApp cluster state.
    *
    * @param desc the streamApp data
    * @param store data store
    * @param clusterCollie the management of cluster
    * @param executionContext execution context
    * @return updated streamApp data
    */
  private[this] def updateState(desc: StreamAppDescription)(
    implicit
    store: DataStore,
    clusterCollie: ClusterCollie,
    meterCache: MeterCache,
    executionContext: ExecutionContext): Future[StreamAppDescription] = {
    Future.successful(desc).flatMap { props =>
      clusterCollie.streamCollie
        .exist(props.name)
        .flatMap {
          if (_) {
            clusterCollie.streamCollie
              .cluster(props.name)
              .filter(_._1.isInstanceOf[StreamClusterInfo])
              .map(_._1.asInstanceOf[StreamClusterInfo] -> None)
          } else {
            // if stream cluster was not created, we initial an empty class
            Future.successful(
              StreamClusterInfo(name = "",
                                imageName = "",
                                jmxPort = 0,
                                state = None,
                                nodeNames = Set.empty,
                                deadNodes = Set.empty) -> None)
          }
        }
        .flatMap {
          case (info, error) =>
            store.addIfPresent[StreamAppDescription](
              name = props.name,
              updater = (previous: StreamAppDescription) =>
                previous.copy(
                  nodeNames = info.nodeNames,
                  deadNodes = info.deadNodes,
                  state = info.state,
                  error = error,
                  metrics = Metrics(meterCache.meters(info).getOrElse("streamapp", Seq.empty))
              )
            )
        }
    }
  }

  /**
    * Check if field was defined
    *
    * @param key the request object key
    * @param field the testing field
    * @param fieldName field name
    * @tparam T field type
    * @return field
    */
  private[this] def checkField[T](key: ObjectKey, field: Option[T], fieldName: String): T =
    field.getOrElse(throw new IllegalArgumentException(RouteUtils.errorMessage(key, fieldName)))

  /**
    * Assert the require streamApp properties
    *
    * @param data streamApp data
    */
  private[this] def assertParameters(data: StreamAppDescription): Unit = {
    CommonUtils.requireNonEmpty(data.name, () => "name fail assert")
    CommonUtils.requireConnectionPort(data.jmxPort)
    Objects.requireNonNull(data.jar)
    CommonUtils.requireNonEmpty(data.from.asJava, () => "from topics fail assert")
    CommonUtils.requireNonEmpty(data.to.asJava, () => "to topics fail assert")
  }

  private[this] def hookOfGet(implicit store: DataStore,
                              clusterCollie: ClusterCollie,
                              meterCache: MeterCache,
                              executionContext: ExecutionContext): HookOfGet[StreamAppDescription] = updateState

  private[this] def hookOfList(implicit store: DataStore,
                               clusterCollie: ClusterCollie,
                               meterCache: MeterCache,
                               executionContext: ExecutionContext): HookOfList[StreamAppDescription] =
    Future.traverse(_)(updateState)

  private[this] def hookOfCreation: HookOfCreation[Creation, StreamAppDescription] =
    (_: String, creation: Creation) => Future.successful(toStreamAppDescription(creation))

  private[this] def hookOfUpdate(
    implicit executionContext: ExecutionContext): HookOfUpdate[Creation, Update, StreamAppDescription] =
    (key: ObjectKey, update: Update, previous: Option[StreamAppDescription]) =>
      Future
        .successful(
          previous.fold(StreamAppDescription(
            name = key.name,
            imageName = update.imageName.getOrElse(IMAGE_NAME_DEFAULT),
            instances = update.nodeNames.fold(checkField(key, update.instances, "instances"))(_.size),
            nodeNames = update.nodeNames.getOrElse(Set.empty),
            deadNodes = Set.empty,
            jar = checkField(key, update.jar, "jar"),
            from = checkField(key, update.from, "from"),
            to = checkField(key, update.to, "to"),
            state = None,
            jmxPort = checkField(key, update.jmxPort, "jmxPort"),
            metrics = Metrics(Seq.empty),
            error = None,
            lastModified = CommonUtils.current(),
            tags = update.tags.getOrElse(Map.empty)
          )) { previous =>
            previous.copy(
              instances = update.instances.getOrElse(previous.instances),
              from = update.from.getOrElse(previous.from),
              to = update.to.getOrElse(previous.to),
              nodeNames = update.nodeNames.getOrElse(previous.nodeNames),
              jmxPort = update.jmxPort.getOrElse(previous.jmxPort),
              tags = update.tags.getOrElse(previous.tags)
            )
          })
        .map { res =>
          if (res.state.isDefined)
            throw new RuntimeException(s"You cannot update property on non-stopped streamApp: $key")
          res
      }

  private[this] def hookBeforeDelete(implicit store: DataStore,
                                     clusterCollie: ClusterCollie,
                                     meterCache: MeterCache,
                                     executionContext: ExecutionContext): HookBeforeDelete = (key: ObjectKey) =>
    // get the latest status first
    store.get[StreamAppDescription](key).flatMap {
      _.fold(Future.unit) { desc =>
        updateState(desc).flatMap { data =>
          if (data.state.isEmpty) {
            // state is not exists, could remove this streamApp
            Future.unit
          } else Future.failed(new RuntimeException(s"You cannot delete a non-stopped streamApp :$key"))
        }
      }
  }

  private[this] def hookOfStart(implicit store: DataStore,
                                adminCleaner: AdminCleaner,
                                nodeCollie: NodeCollie,
                                clusterCollie: ClusterCollie,
                                workerCollie: WorkerCollie,
                                brokerCollie: BrokerCollie,
                                fileStore: FileStore,
                                executionContext: ExecutionContext): HookOfStart[StreamAppDescription] =
    (key: ObjectKey) =>
      store.value[StreamAppDescription](key).flatMap { data =>
        assertParameters(data)
        // we assume streamApp has following conditions:
        // 1) use any available node of worker cluster to run streamApp
        // 2) use one from/to pair topic (multiple from/to topics will need to discuss flow)
        clusterCollie.streamCollie.exist(data.name).flatMap {
          if (_) {
            // stream cluster exists, get current cluster
            clusterCollie.streamCollie
              .cluster(data.name)
              .filter(_._1.isInstanceOf[StreamClusterInfo])
              .map(_._1.asInstanceOf[StreamClusterInfo])
              .flatMap(
                clusterInfo =>
                  store.addIfPresent[StreamAppDescription](
                    key = key,
                    updater = (previous: StreamAppDescription) => previous.copy(state = clusterInfo.state)
                ))
          } else {
            // initial the cluster creation request
            val req = Creation(
              name = data.name,
              imageName = data.imageName,
              jar = data.jar,
              from = data.from,
              to = data.to,
              jmxPort = data.jmxPort,
              instances = data.instances,
              nodeNames = data.nodeNames,
              tags = data.tags
            )
            // check cluster creation rules
            RouteUtils
              .basicCheckOfCluster[Creation, StreamClusterInfo](nodeCollie, clusterCollie, req)
              .flatMap(
                _ =>
                  // get the broker info and topic info from worker cluster name
                  CollieUtils
                    .both(Some(data.jar.group))
                    // get broker props from worker cluster
                    .map { case (_, topicAdmin, _, _) => topicAdmin.connectionProps }
                    .flatMap { bkProps =>
                      fileStore
                        .fileInfo(data.jar.group, data.jar.name)
                        .map(_.url)
                        .flatMap { url =>
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
                                CommonUtils
                                  .requireNonEmpty(all.filter(n => data.nodeNames.contains(n.name)).asJava)
                                  .asScala
                                  .toSet
                            }
                            .flatMap(
                              nodes =>
                                clusterCollie.streamCollie.creator
                                  .clusterName(data.name)
                                  .nodeNames(nodes.map(_.name))
                                  .imageName(IMAGE_NAME_DEFAULT)
                                  .jarUrl(url.toString)
                                  .appId(data.name)
                                  .brokerProps(bkProps)
                                  .fromTopics(data.from)
                                  .toTopics(data.to)
                                  .enableExactlyOnce(data.exactlyOnce)
                                  .threadPool(executionContext)
                                  .create())
                        }
                        .map(_.state -> None)
                    }
                    // if start failed (no matter why), we change the status to "DEAD"
                    // in order to identify "fail started"(status: DEAD) and "successful stopped"(status = None)
                    .recover {
                      case ex: Throwable =>
                        log.error(s"start streamApp failed: ", ex)
                        Some(ContainerState.DEAD.name) -> Some(ex.getMessage)
                    }
                    .flatMap {
                      case (state, error) =>
                        store.addIfPresent[StreamAppDescription](
                          key = key,
                          updater = (previous: StreamAppDescription) => previous.copy(state = state, error = error)
                        )
                  })
          }
        }
    }

  private[this] def hookOfStop(implicit store: DataStore,
                               clusterCollie: ClusterCollie,
                               executionContext: ExecutionContext): HookOfStop[StreamAppDescription] =
    (key: ObjectKey) =>
      store.value[StreamAppDescription](key).flatMap { data =>
        clusterCollie.streamCollie.exist(data.name).flatMap {
          if (_) {
            // if remove failed, we log the exception and return "DEAD" state
            clusterCollie.streamCollie
              .remove(data.name)
              .map(_ => None -> None)
              .recover {
                case ex: Throwable =>
                  log.error(s"failed to stop streamApp for $key.", ex)
                  Some(ContainerState.DEAD.name) -> Some(ex.getMessage)
              }
              .flatMap {
                case (state, error) =>
                  store.addIfPresent[StreamAppDescription](
                    key = key,
                    updater = (previous: StreamAppDescription) => previous.copy(state = state, error = error)
                  )
              }
          } else {
            // stream cluster not exists, update store only
            store.addIfPresent[StreamAppDescription](
              key = key,
              updater = (previous: StreamAppDescription) => previous.copy(state = None, error = None)
            )
          }
        }
    }

  def apply(implicit store: DataStore,
            adminCleaner: AdminCleaner,
            nodeCollie: NodeCollie,
            clusterCollie: ClusterCollie,
            workerCollie: WorkerCollie,
            brokerCollie: BrokerCollie,
            fileStore: FileStore,
            meterCache: MeterCache,
            executionContext: ExecutionContext): server.Route =
    RouteUtils.route[Creation, Update, StreamAppDescription](
      root = STREAM_PREFIX_PATH,
      enableGroup = false,
      hookOfCreation = hookOfCreation,
      hookOfUpdate = hookOfUpdate,
      hookBeforeDelete = hookBeforeDelete,
      hookOfGet = hookOfGet,
      hookOfList = hookOfList,
      hookOfStart = hookOfStart,
      hookOfStop = hookOfStop
    )
}
