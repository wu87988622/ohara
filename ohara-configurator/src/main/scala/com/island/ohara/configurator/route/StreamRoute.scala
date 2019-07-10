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

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.agent.{BrokerCollie, ClusterCollie, NodeCollie, WorkerCollie}
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.StreamApi._
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.jar.JarStore
import com.island.ohara.configurator.store.{DataStore, MeterCache}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

private[configurator] object StreamRoute {

  private[this] val log = LoggerFactory.getLogger(StreamRoute.getClass)

  /**
    * save the streamApp properties
    *
    * @param req the property request
    * @return '''StreamApp''' object
    */
  private[this] def toStore(req: Creation): StreamAppDescription =
    StreamAppDescription(
      name = req.name,
      imageName = req.imageName,
      instances = if (req.nodeNames.isEmpty) req.instances else req.nodeNames.size,
      nodeNames = req.nodeNames,
      deadNodes = Set.empty,
      jar = req.jar,
      from = req.from,
      to = req.to,
      state = None,
      jmxPort = req.jmxPort,
      metrics = Metrics(Seq.empty),
      error = None,
      lastModified = CommonUtils.current(),
      tags = req.tags
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
              props.name,
              previous =>
                Future.successful(
                  previous.copy(
                    nodeNames = info.nodeNames,
                    deadNodes = info.deadNodes,
                    state = info.state,
                    error = error,
                    metrics = Metrics(meterCache.meters(info).getOrElse("streamapp", Seq.empty))
                  )
              )
            )
        }
    }
  }

  /**
    * Check if field was defined
    *
    * @param name the request object name
    * @param field the testing field
    * @param fieldName field name
    * @tparam T field type
    * @return field
    */
  private[this] def checkField[T](name: String, field: Option[T], fieldName: String): T =
    field.getOrElse(throw new IllegalArgumentException(RouteUtils.errorMessage(name, fieldName)))

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

  def apply(implicit store: DataStore,
            adminCleaner: AdminCleaner,
            nodeCollie: NodeCollie,
            clusterCollie: ClusterCollie,
            workerCollie: WorkerCollie,
            brokerCollie: BrokerCollie,
            jarStore: JarStore,
            meterCache: MeterCache,
            executionContext: ExecutionContext): server.Route =
    pathPrefix(STREAM_PREFIX_PATH) {
      pathEnd {
        complete(StatusCodes.BadRequest -> "wrong uri")
      } ~
        //StreamApp Property Page
        RouteUtils.basicRoute[Creation, Update, StreamAppDescription](
          root = STREAM_PROPERTY_PREFIX_PATH,
          hookOfCreation = (req: Creation) => Future.successful(toStore(req)),
          hookOfUpdate = (name: String, req: Update, previousOption: Option[StreamAppDescription]) => {
            val updateReq = previousOption.fold(StreamAppDescription(
              name = name,
              imageName = req.imageName.getOrElse(IMAGE_NAME_DEFAULT),
              instances = req.nodeNames.fold(checkField(name, req.instances, "instances"))(_.size),
              nodeNames = req.nodeNames.getOrElse(Set.empty),
              deadNodes = Set.empty,
              jar = checkField(name, req.jar, "jar"),
              from = checkField(name, req.from, "from"),
              to = checkField(name, req.to, "to"),
              state = None,
              jmxPort = checkField(name, req.jmxPort, "jmxPort"),
              metrics = Metrics(Seq.empty),
              error = None,
              lastModified = CommonUtils.current(),
              tags = req.tags.getOrElse(Set.empty)
            )) { previous =>
              previous.copy(
                instances = req.instances.getOrElse(previous.instances),
                from = req.from.getOrElse(previous.from),
                to = req.to.getOrElse(previous.to),
                nodeNames = req.nodeNames.getOrElse(previous.nodeNames),
                jmxPort = req.jmxPort.getOrElse(previous.jmxPort),
                tags = req.tags.getOrElse(previous.tags)
              )
            }
            if (updateReq.state.isDefined)
              throw new RuntimeException(s"You cannot update property on non-stopped streamApp: $name")
            else Future.successful(updateReq)
          },
          hookBeforeDelete = (name: String) =>
            // get the latest status first
            store.get[StreamAppDescription](name).flatMap {
              _.fold(Future.successful(name)) { desc =>
                updateState(desc).flatMap { data =>
                  if (data.state.isEmpty) {
                    // state is not exists, could remove this streamApp
                    Future.successful(name)
                  } else Future.failed(new RuntimeException(s"You cannot delete a non-stopped streamApp :$name"))
                }
              }
          },
          hookOfGet = (response: StreamAppDescription) => updateState(response),
          hookOfList = (responses: Seq[StreamAppDescription]) => Future.traverse(responses)(updateState)
        ) ~ pathPrefix(Segment) { name =>
        pathEnd {
          complete(StatusCodes.BadRequest -> "wrong uri")
        } ~
          // start streamApp
          path(START_COMMAND) {
            put {
              complete(store.value[StreamAppDescription](name).flatMap { data =>
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
                      .flatMap(clusterInfo =>
                        store.addIfPresent[StreamAppDescription](
                          name,
                          data => Future.successful(data.copy(state = clusterInfo.state))
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
                            .flatMap {
                              bkProps =>
                                jarStore
                                  .jarInfo(data.jar.group, data.jar.name)
                                  .map(_.url)
                                  .flatMap {
                                    url =>
                                      nodeCollie
                                        .nodes()
                                        .map {
                                          all =>
                                            if (CommonUtils.isEmpty(data.nodeNames.asJava)) {
                                              // Check instance first
                                              // Here we will check the following conditions:
                                              // 1. instance should be positive
                                              // 2. available nodes should be bigger than instance (one node runs one instance)
                                              if (all.size < data.instances)
                                                throw new IllegalArgumentException(
                                                  s"cannot run streamApp. expect: ${data.instances}, actual: ${all.size}")
                                              Random
                                                .shuffle(all)
                                                .take(CommonUtils.requirePositiveInt(data.instances))
                                                .toSet
                                            } else
                                              // if require node name is not in nodeCollie, do not take that node
                                              CommonUtils
                                                .requireNonEmpty(
                                                  all.filter(n => data.nodeNames.contains(n.name)).asJava)
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
                                  name,
                                  data => Future.successful(data.copy(state = state, error = error))
                                )
                          })
                  }
                }

              })
            }
          } ~
          // stop streamApp
          path(STOP_COMMAND) {
            put {
              complete(
                store.value[StreamAppDescription](name).flatMap { data =>
                  clusterCollie.streamCollie.exist(data.name).flatMap {
                    if (_) {
                      // if remove failed, we log the exception and return "DEAD" state
                      clusterCollie.streamCollie
                        .remove(data.name)
                        .map(_ => None -> None)
                        .recover {
                          case ex: Throwable =>
                            log.error(s"failed to stop streamApp for $name.", ex)
                            Some(ContainerState.DEAD.name) -> Some(ex.getMessage)
                        }
                        .flatMap {
                          case (state, error) =>
                            store.addIfPresent[StreamAppDescription](
                              name,
                              data => Future.successful(data.copy(state = state, error = error))
                            )
                        }
                    } else {
                      // stream cluster not exists, update store only
                      store.addIfPresent[StreamAppDescription](
                        name,
                        data => Future.successful(data.copy(state = None, error = None))
                      )
                    }
                  }
                }
              )
            }
          }
      }
    }
}
