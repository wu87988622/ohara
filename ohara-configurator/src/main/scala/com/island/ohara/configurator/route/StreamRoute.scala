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
import com.island.ohara.agent.{BrokerCollie, Crane, WorkerCollie}
import com.island.ohara.client.configurator.v0.JarApi.JarInfo
import com.island.ohara.client.configurator.v0.StreamApi._
import com.island.ohara.client.configurator.v0.{Parameters, StreamApi}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.jar.JarStore
import com.island.ohara.configurator.store.DataStore
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

private[configurator] object StreamRoute {

  private[this] val log = LoggerFactory.getLogger(StreamRoute.getClass)

  /**
    * save the streamApp properties
    *
    * @param req the property request
    * @param streamJar the list response
    * @param jarInfo jar information of streamApp
    * @return '''StreamApp''' object
    */
  private[this] def toStore(req: StreamPropertyRequest, streamJar: StreamJar, jarInfo: JarInfo): StreamAppDescription =
    StreamAppDescription(
      workerClusterName = streamJar.workerClusterName,
      id = CommonUtils.uuid(),
      name = req.name.getOrElse("Untitled stream app"),
      instances = req.instances.getOrElse(1),
      jarInfo = jarInfo,
      from = req.from.getOrElse(Seq.empty),
      to = req.to.getOrElse(Seq.empty),
      state = None,
      error = None,
      lastModified = CommonUtils.current()
    )

  /**
    * Update the streamApp cluster state.
    *
    * @param id the streamApp id
    * @param store data store
    * @param crane manager of container clusters
    * @param executionContext execution context
    * @return updated streamApp data
    */
  private[this] def updateState(id: String)(implicit
                                            store: DataStore,
                                            crane: Crane,
                                            executionContext: ExecutionContext): Future[StreamAppDescription] = {
    store.value[StreamAppDescription](id).flatMap { props =>
      crane
        .get(formatClusterName(props.id))
        .filter(_._1.isInstanceOf[StreamClusterInfo])
        .map(_._1.asInstanceOf[StreamClusterInfo].state -> None)
        // if stream cluster was not created, we do nothing
        .recover {
          case ex: Throwable =>
            log.warn(s"stream cluster not exists yet: ", ex)
            None -> None
        }
        .flatMap {
          case (state, error) =>
            store.update[StreamAppDescription](
              props.id,
              previous => Future.successful(previous.copy(state = state, error = error)))
        }
    }
  }

  /**
    * Assert the require streamApp properties
    *
    * @param data streamApp data
    */
  private[this] def assertParameters(data: StreamAppDescription): Unit = {
    CommonUtils.requireNonEmpty(
      data.workerClusterName,
      () => "workerClusterName fail assert"
    )
    CommonUtils.requireNonEmpty(data.id, () => "id fail assert")
    require(data.instances > 0, "instances should bigger than 0")
    Objects.requireNonNull(data.jarInfo)
    CommonUtils.requireNonEmpty(data.from.asJava, () => "from topics fail assert")
    CommonUtils.requireNonEmpty(data.to.asJava, () => "to topics fail assert")
  }

  def apply(implicit store: DataStore,
            workerCollie: WorkerCollie,
            brokerCollie: BrokerCollie,
            jarStore: JarStore,
            urlGenerator: UrlGenerator,
            crane: Crane,
            executionContext: ExecutionContext): server.Route =
    pathPrefix(STREAM_PREFIX_PATH) {
      pathEnd {
        complete(StatusCodes.BadRequest -> "wrong uri")
      } ~
        pathPrefix(STREAM_LIST_PREFIX_PATH) {
          //upload jars
          post {
            //see https://github.com/akka/akka-http/issues/1216#issuecomment-311973943
            toStrictEntity(1.seconds) {
              formFields(Parameters.CLUSTER_NAME.?) { reqName =>
                storeUploadedFiles(StreamApi.INPUT_KEY, StreamApi.saveTmpFile) { files =>
                  complete(
                    // here we try to find the pre-defined wk if not assigned by request
                    CollieUtils.workerClient(reqName).map(_._1.name).map { wkName =>
                      log.debug(s"worker: $wkName, files: ${files.map(_._1.fileName)}")
                      Future
                        .sequence(files.map {
                          case (metadata, file) =>
                            if (file.length() > StreamApi.MAX_FILE_SIZE) {
                              throw new RuntimeException(
                                s"the file : ${metadata.fileName} size is bigger than ${StreamApi.MAX_FILE_SIZE / 1024 / 1024} MB."
                              )
                            }
                            jarStore.add(file, s"${metadata.fileName}").flatMap { jarInfo =>
                              store.add(
                                StreamJar(
                                  wkName,
                                  jarInfo.id,
                                  jarInfo.name,
                                  CommonUtils.current()
                                ))
                            }
                        })
                        .map { reps =>
                          //delete temp jars after success
                          files.foreach {
                            case (_, file) => file.deleteOnExit()
                          }
                          reps
                        }
                    }
                  )
                }
              }
            }
          } ~
            //return list
            get {
              parameter(Parameters.CLUSTER_NAME.?) { wkName =>
                complete(
                  store
                    .values[StreamJar]()
                    // filter specific cluster only, or return all otherwise
                    .map(_.filter(jarInfo => wkName.isEmpty || jarInfo.workerClusterName == wkName.get))
                )
              }
            } ~
            // need id to delete / update jar
            path(Segment) { id =>
              //delete jar
              delete {
                complete(store.values[StreamAppDescription]().map { streamApps =>
                  // check the jar is not used in any streamApp which is used in pipeline
                  if (streamApps.exists(_.jarInfo.id == id)) {
                    throw new IllegalArgumentException(s"The id:$id is used by pipeline")
                  } else {
                    store.remove[StreamJar](id).flatMap { _ =>
                      jarStore
                        .exist(id)
                        .map(if (_) {
                          jarStore.remove(id)
                          true
                        } else true)
                        .map(_ => StatusCodes.NoContent)
                    }
                  }
                })
              } ~
                //update jar name
                put {
                  entity(as[StreamListRequest]) { req =>
                    if (CommonUtils.isEmpty(req.jarName))
                      throw new IllegalArgumentException(s"Require jarName")
                    complete(jarStore.rename(id, req.jarName).flatMap { jarInfo =>
                      store.update[StreamJar](
                        id,
                        previous =>
                          Future.successful(
                            previous.copy(
                              name = jarInfo.name,
                              lastModified = CommonUtils.current()
                            )
                        )
                      )
                    })
                  }
                }
            }
        } ~
        //StreamApp Property Page
        pathPrefix(STREAM_PROPERTY_PREFIX_PATH) {
          pathEnd {
            // create property
            post {
              entity(as[StreamPropertyRequest]) { req =>
                complete(jarStore.jarInfo(req.jarId).flatMap { jarInfo =>
                  store
                    .value[StreamJar](req.jarId)
                    .flatMap(streamJar =>
                      store.add[StreamAppDescription](
                        toStore(req, streamJar, jarInfo)
                    ))
                })
              }
            }
          } ~
            path(Segment) { id =>
              // delete property
              delete {
                complete(
                  // get the latest status first
                  store.get[StreamAppDescription](id).flatMap {
                    _.map { desc =>
                      updateState(desc.id).flatMap { data =>
                        if (data.state.isEmpty) {
                          // state is not exists, could remove this streamApp
                          store.remove[StreamAppDescription](id).map(_ => StatusCodes.NoContent)
                        } else Future.failed(new RuntimeException(s"You cannot delete a non-stopped streamApp :$id"))
                      }
                    }.getOrElse(Future.successful(StatusCodes.NoContent))
                  }
                )
              } ~
                // get property
                get {
                  complete(updateState(id))
                } ~
                // update property
                put {
                  entity(as[StreamPropertyRequest]) { req =>
                    complete(
                      store.update[StreamAppDescription](
                        id,
                        previous =>
                          if (previous.state.isDefined)
                            throw new RuntimeException(s"You cannot update property on non-stopped streamApp")
                          else
                            Future.successful(
                              previous.copy(
                                name = req.name.getOrElse(previous.name),
                                instances = req.instances.getOrElse(previous.instances),
                                from = req.from.getOrElse(previous.from),
                                to = req.to.getOrElse(previous.to),
                                lastModified = CommonUtils.current()
                              )
                          )
                      )
                    )
                  }
                }
            }
        } ~ pathPrefix(Segment) { id =>
        pathEnd {
          complete(StatusCodes.BadRequest -> "wrong uri")
        } ~
          // start streamApp
          path(START_COMMAND) {
            put {
              complete(store.value[StreamAppDescription](id).flatMap { data =>
                assertParameters(data)
                // we assume streamApp has following conditions:
                // 1) use any available node of worker cluster to run streamApp
                // 2) use one from/to pair topic (multiple from/to topics will need to discuss flow)

                // get the broker info and topic info from worker cluster name
                CollieUtils
                  .both(Some(data.workerClusterName))
                  // get broker props from worker cluster
                  .map { case (_, topicAdmin, _, _) => topicAdmin.connectionProps }
                  .flatMap { bkProps =>
                    urlGenerator
                      .url(data.jarInfo.id)
                      .flatMap {
                        url =>
                          crane.exist(formatClusterName(data.id)).flatMap {
                            if (_) {
                              // stream cluster exists, get current cluster
                              crane
                                .get(formatClusterName(data.id))
                                .filter(_._1.isInstanceOf[StreamClusterInfo])
                                .map(_._1.asInstanceOf[StreamClusterInfo])
                            } else {
                              crane
                                .streamWarehouse()
                                .creator()
                                .clusterName(formatClusterName(data.id))
                                .instances(data.instances)
                                .imageName(IMAGE_NAME_DEFAULT)
                                .jarUrl(url.toString)
                                .appId(formatAppId(data.id))
                                .brokerProps(bkProps)
                                .fromTopics(data.from)
                                .toTopics(data.to)
                                .create()
                            }
                          }
                      }
                      .map(_.state -> None)
                  }
                  // if start failed (no matter why), we change the status to "EXITED"
                  // in order to identify "fail started"(status: EXITED) and "successful stopped"(status = None)
                  .recover {
                    case ex: Throwable =>
                      log.error(s"start streamApp failed: ", ex)
                      Some(ContainerState.EXITED.name) -> Some(ex.getMessage)
                  }
                  .flatMap {
                    case (state, error) =>
                      store.update[StreamAppDescription](
                        id,
                        data => Future.successful(data.copy(state = state, error = error))
                      )
                  }
              })
            }
          } ~
          // stop streamApp
          path(STOP_COMMAND) {
            put {
              complete(
                store.value[StreamAppDescription](id).flatMap { data =>
                  crane.exist(formatClusterName(data.id)).flatMap {
                    if (_) {
                      // if remove failed, we log the exception and return "EXITED" state
                      crane
                        .remove(formatClusterName(data.id))
                        .map(_ => None -> None)
                        .recover {
                          case ex: Throwable =>
                            log.error(s"failed to stop streamApp for $id.", ex)
                            Some(ContainerState.EXITED.name) -> Some(ex.getMessage)
                        }
                        .flatMap {
                          case (state, error) =>
                            store.update[StreamAppDescription](
                              id,
                              data => Future.successful(data.copy(state = state, error = error))
                            )
                        }
                    } else {
                      // stream cluster not exists, update store only
                      store.update[StreamAppDescription](
                        id,
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
