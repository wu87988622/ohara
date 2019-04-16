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

import java.io.File
import java.util.Objects

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.agent.{BrokerCollie, Crane, WorkerCollie}
import com.island.ohara.client.configurator.v0.PipelineApi.Pipeline
import com.island.ohara.client.configurator.v0.StreamApi._
import com.island.ohara.client.configurator.v0.{JarApi, StreamApi}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.jar.JarStore
import com.island.ohara.configurator.route.RouteUtils._
import com.island.ohara.configurator.store.DataStore
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._

private[configurator] object StreamRoute {

  private[this] val log = LoggerFactory.getLogger(StreamRoute.getClass)

  /**
    * save the streamApp properties
    *
    * @param pipelineId the pipeline id that streamApp sited in
    * @param clusterName the streamApp cluster name
    * @param streamId unique uuid for streamApp
    * @param name customize name
    * @param instances number of streamApp running
    * @param jarInfo jar of streamApp running with
    * @param from streamApp consume with
    * @param to streamApp produce to
    * @param lastModified last modified time for this data
    * @return '''StreamApp''' object
    */
  private[this] def toStore(pipelineId: String,
                            clusterName: String = "",
                            streamId: String,
                            // default streamApp component name
                            name: String = "Untitled stream app",
                            // default instances
                            instances: Int = 0,
                            jarInfo: JarApi.JarInfo,
                            from: Seq[String] = Seq.empty,
                            to: Seq[String] = Seq.empty,
                            lastModified: Long): StreamAppDescription =
    StreamAppDescription(
      pipelineId = pipelineId,
      clusterName = clusterName,
      id = streamId,
      name = name,
      instances = instances,
      jarInfo = jarInfo,
      from = from,
      to = to,
      state = None,
      lastModified = lastModified
    )

  /**
    * Assert the require streamApp properties
    *
    * @param data streamApp data
    */
  private[this] def assertParameters(data: StreamAppDescription): Unit = {
    CommonUtils.requireNonEmpty(
      data.pipelineId,
      () => "pipelineId fail assert"
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
            crane: Crane,
            executionContext: ExecutionContext): server.Route =
    pathPrefix(STREAM_PREFIX_PATH) {
      pathEnd {
        complete(StatusCodes.BadRequest -> "wrong uri")
      } ~
        pathPrefix(STREAM_LIST_PREFIX_PATH) {
          pathEnd {
            complete(StatusCodes.BadRequest -> "wrong uri")
          } ~
            path(Segment) { id =>
              //upload jars
              post {
                storeUploadedFiles(StreamApi.INPUT_KEY, StreamApi.saveTmpFile) { files =>
                  complete(
                    Future
                      .sequence(files.map {
                        case (metadata, file) =>
                          if (file.length() > StreamApi.MAX_FILE_SIZE) {
                            throw new RuntimeException(
                              s"the file : ${metadata.fileName} size is bigger than ${StreamApi.MAX_FILE_SIZE / 1024 / 1024} MB."
                            )
                          }
                          jarStore.add(file, s"${metadata.fileName}")
                      })
                      .map { jarInfos =>
                        val jars = Future.sequence(jarInfos.map { jarInfo =>
                          val time = CommonUtils.current()
                          val streamId = CommonUtils.uuid()
                          store
                            .add(
                              toStore(
                                // In here, id is pipelineId which passed from API
                                pipelineId = id,
                                streamId = streamId,
                                jarInfo = jarInfo,
                                lastModified = time
                              )
                            )
                            .map { data =>
                              StreamListResponse(
                                data.id,
                                data.name,
                                data.jarInfo.name,
                                data.lastModified
                              )
                            }
                        })
                        //delete temp jars after success
                        files.foreach {
                          case (_, file) => file.deleteOnExit()
                        }
                        jars
                      }
                  )
                }
              } ~
                //return list
                get {
                  complete(
                    store
                      .values[StreamAppDescription]
                      .map(
                        // In here, id is pipelineId which passed from API
                        _.filter(f => f.pipelineId.equals(id)).map(
                          data =>
                            StreamListResponse(
                              data.id,
                              data.name,
                              data.jarInfo.name,
                              data.lastModified
                          )
                        )
                      )
                  )
                } ~
                //delete jar
                delete {
                  // In here, id is streamApp id which passed from API
                  assertNotRelated2Pipeline(id)
                  complete(store.remove[StreamAppDescription](id).flatMap { data =>
                    jarStore
                      .remove(data.jarInfo.id)
                      .map(
                        _ =>
                          StreamListResponse(
                            data.id,
                            data.name,
                            data.jarInfo.name,
                            data.lastModified
                        )
                      )
                  })
                } ~
                //update jar name
                put {
                  entity(as[StreamListRequest]) { req =>
                    if (CommonUtils.isEmpty(req.jarName))
                      throw new IllegalArgumentException(s"Require jarName")
                    complete(
                      store
                        .value[StreamAppDescription](id)
                        .flatMap { data =>
                          // get the old jar url
                          jarStore
                            .url(data.jarInfo.id)
                            .flatMap { url =>
                              val tmpFile =
                                new File(StreamApi.TMP_ROOT, CommonUtils.randomString(5))
                              // download the jar file from remote ftp server by URL
                              (url #> tmpFile).!!
                              // upload the jar with new jar name
                              jarStore.add(tmpFile, req.jarName)
                            }
                            .flatMap { jarInfo =>
                              store.update[StreamAppDescription](
                                id,
                                previous =>
                                  Future.successful(
                                    toStore(
                                      pipelineId = previous.pipelineId,
                                      streamId = previous.id,
                                      name = previous.name,
                                      instances = previous.instances,
                                      jarInfo = jarInfo,
                                      from = previous.from,
                                      to = previous.to,
                                      lastModified = CommonUtils.current()
                                    )
                                )
                              )
                            }
                        }
                        .map(
                          data =>
                            StreamListResponse(
                              data.id,
                              data.name,
                              data.jarInfo.name,
                              data.lastModified
                          )
                        )
                    )
                  }
                }
            }
        } ~
        //StreamApp Property Page
        pathPrefix(STREAM_PROPERTY_PREFIX_PATH) {
          pathEnd {
            complete(StatusCodes.BadRequest -> "wrong uri")
          } ~
            path(Segment) { id =>
              //add property is impossible, we need streamApp id first
              post {
                complete(
                  StatusCodes.BadRequest ->
                    "You should upload a jar first and use PUT method to update properties."
                )
              } ~
                // delete property is useless, we handle this in StreamApp List -> DELETE method
                delete {
                  complete(
                    StatusCodes.BadRequest ->
                      "You cannot delete properties only. Please use DELETE method in StreamApp List API instead. "
                  )
                } ~
                // get property
                get {
                  complete(
                    store
                      .value[StreamAppDescription](id)
                      .map(
                        data =>
                          StreamPropertyResponse(
                            id,
                            data.jarInfo.name,
                            data.name,
                            data.from,
                            data.to,
                            data.instances,
                            data.lastModified
                        )
                      )
                  )
                } ~
                // update
                put {
                  entity(as[StreamPropertyRequest]) { req =>
                    complete(
                      store
                        .value[StreamAppDescription](id)
                        .flatMap { data =>
                          val newData = toStore(
                            pipelineId = data.pipelineId,
                            // we use streamApp unique id as the cluster name
                            clusterName = data.id.replaceAll("-", ""),
                            streamId = data.id,
                            name = req.name,
                            instances = req.instances,
                            jarInfo = data.jarInfo,
                            from = req.from,
                            to = req.to,
                            lastModified = CommonUtils.current()
                          )
                          store.update[StreamAppDescription](
                            id,
                            _ => Future.successful(newData)
                          )
                        }
                        .map(
                          newData =>
                            StreamPropertyResponse(
                              id,
                              newData.jarInfo.name,
                              newData.name,
                              newData.from,
                              newData.to,
                              newData.instances,
                              newData.lastModified
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
              complete(store.value[StreamAppDescription](id).flatMap {
                data =>
                  assertParameters(data)
                  // we assume streamApp has following conditions:
                  // 1) use any available node of worker cluster to run streamApp
                  // 2) use one from/to pair topic (multiple from/to topics will need to discuss flow)

                  // get the broker info and topic info from worker cluster
                  // TODO : we should get the workerClusterName by url request, see
                  // TODO : https://github.com/oharastream/ohara/issues/751
                  store
                    .value[Pipeline](data.pipelineId)
                    .flatMap { pipeline =>
                      CollieUtils.both(
                        Some(pipeline.workerClusterName)
                      )
                    }
                    // get broker props from worker cluster
                    .map { case (_, topicAdmin, _, _) => topicAdmin.connectionProps }
                    .flatMap { bkProps =>
                      jarStore
                        .url(data.jarInfo.id)
                        .flatMap {
                          val streamAppId =
                            StreamApi.formatAppId(data.id)
                          url =>
                            crane
                              .streamWarehouse()
                              .creator()
                              .clusterName(data.clusterName)
                              .instances(data.instances)
                              .imageName(STREAMAPP_IMAGE)
                              .jarUrl(url.toString)
                              .appId(streamAppId)
                              .brokerProps(bkProps)
                              .fromTopics(data.from)
                              .toTopics(data.to)
                              .create()
                        }
                        // if start failed (no matter why), we change the status to "EXITED"
                        // in order to identify "fail started"(status: EXITED) and "successful stopped"(status = None)
                        .recover {
                          case ex: Throwable =>
                            log.error(s"start streamApp failed : ${ex.getMessage}")
                            StreamClusterInfo(
                              name = data.id,
                              imageName = STREAMAPP_IMAGE,
                              state = Some(ContainerState.EXITED.name)
                            )
                        }
                    }
                    .flatMap { clusterInfo =>
                      store.update[StreamAppDescription](
                        id,
                        data => Future.successful(data.copy(state = clusterInfo.state))
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
                  crane
                  // if remove failed, we throw the exception instead of swallowing it
                    .remove(data.clusterName)
                    .map(
                      _ =>
                        StreamClusterInfo(
                          name = data.clusterName,
                          imageName = STREAMAPP_IMAGE,
                          state = None
                      ))
                    .flatMap { info =>
                      store.update[StreamAppDescription](
                        id,
                        data => Future.successful(data.copy(state = info.state))
                      )
                    }
                }
              )
            }
          } ~
          path(STATUS_COMMAND) {
            put {
              complete(
                store
                  .value[StreamAppDescription](id)
                  .flatMap { data =>
                    crane.get(data.clusterName).map(_._1.asInstanceOf[StreamClusterInfo])
                  }
                  .flatMap { info =>
                    store.update[StreamAppDescription](
                      id,
                      data => Future.successful(data.copy(state = info.state))
                    )
                  }
              )
            }
          }
      }
    }
}
