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

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.agent.{BrokerCollie, Crane}
import com.island.ohara.client.configurator.v0.PipelineApi.Pipeline
import com.island.ohara.client.configurator.v0.StreamApi._
import com.island.ohara.client.configurator.v0.TopicApi.TopicInfo
import com.island.ohara.client.configurator.v0.{JarApi, StreamApi}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.jar.JarStore
import com.island.ohara.configurator.route.RouteUtils._
import com.island.ohara.configurator.store.DataStore
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._

private[configurator] object StreamRoute {

  private[this] val log = LoggerFactory.getLogger(StreamRoute.getClass)

  private[this] def toStore(pipelineId: String,
                            streamId: String,
                            name: String = "Untitled stream app",
                            instances: Int = 1,
                            jarInfo: JarApi.JarInfo,
                            fromTopics: Seq[String] = Seq.empty,
                            toTopics: Seq[String] = Seq.empty,
                            lastModified: Long): StreamApp =
    StreamApp(pipelineId = pipelineId,
              id = streamId,
              name = name,
              instances = instances,
              jarInfo = jarInfo,
              fromTopics = fromTopics,
              toTopics = toTopics,
              lastModified = lastModified)

  private[this] def assertParameters(data: StreamApp): Boolean = {
    def isNotNullOrEmpty(str: String): Boolean = { str != null && str.nonEmpty }
    data.pipelineId.nonEmpty &&
    data.id.nonEmpty &&
    data.name.nonEmpty &&
    data.instances > 0 &&
    data.fromTopics.nonEmpty &&
    data.fromTopics.forall(isNotNullOrEmpty) &&
    data.toTopics.nonEmpty &&
    data.toTopics.forall(isNotNullOrEmpty)
  }

  def apply(implicit store: DataStore,
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
                              s"the file : ${metadata.fileName} size is bigger than ${StreamApi.MAX_FILE_SIZE / 1024 / 1024} MB.")
                          }
                          jarStore.add(file, s"${metadata.fileName}")
                      })
                      .map { jarInfos =>
                        val jars = Future.sequence(jarInfos.map { jarInfo =>
                          val time = CommonUtils.current()
                          val streamId = CommonUtils.uuid()
                          store
                            .add(toStore(pipelineId = id, streamId = streamId, jarInfo = jarInfo, lastModified = time))
                            .map { data =>
                              StreamListResponse(data.id, data.name, data.jarInfo.name, data.lastModified)
                            }
                        })
                        //delete temp jars after success
                        files.foreach { case (_, file) => file.deleteOnExit() }
                        jars
                      }
                  )
                }
              } ~
                //return list
                get {
                  complete(
                    store
                      .values[StreamApp]
                      .map(_.filter(f => f.pipelineId.equals(id))
                      //note : this id is given by UI (pipeline_id)
                        .map(data => StreamListResponse(data.id, data.name, data.jarInfo.name, data.lastModified))))
                } ~
                //delete jar
                delete {
                  //TODO : check streamApp is not at running state...by Sam
                  assertNotRelated2Pipeline(id)
                  val result = for {
                    f1 <- store.remove[StreamApp](id)
                    f2 <- jarStore.remove(f1.jarInfo.id)
                  } yield f1
                  complete(
                    result.map(data => StreamListResponse(data.id, data.name, data.jarInfo.name, data.lastModified)))
                } ~
                //update jar name
                put {
                  entity(as[StreamListRequest]) { req =>
                    if (req.jarName == null || req.jarName.isEmpty)
                      throw new IllegalArgumentException(s"Require jarName")
                    val f = new File(StreamApi.TMP_ROOT, CommonUtils.randomString(5))
                    val result = for {
                      f1 <- store.value[StreamApp](id)
                      f2 <- jarStore.url(f1.jarInfo.id)
                      f3 <- {
                        //download the jar file from remote ftp server by URL...use more readable code ?...by Sam
                        { f2 #> f !! }
                        jarStore.add(f, req.jarName)
                      }
                      f4 <- jarStore.remove(f1.jarInfo.id)
                      f5 <- store.update[StreamApp](
                        id,
                        previous =>
                          Future.successful(
                            toStore(previous.pipelineId,
                                    previous.id,
                                    previous.name,
                                    previous.instances,
                                    f3,
                                    previous.fromTopics,
                                    previous.toTopics,
                                    CommonUtils.current()))
                      )
                    } yield f5
                    complete(result.map(newData =>
                      StreamListResponse(newData.id, newData.name, newData.jarInfo.name, newData.lastModified)))
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
                complete(StatusCodes.BadRequest ->
                  "You should upload a jar first and use PUT method to update properties.")
              } ~
                // delete property is useless, we handle this in StreamApp List -> DELETE method
                delete {
                  complete(StatusCodes.BadRequest ->
                    "You cannot delete properties only. Please use DELETE method in StreamApp List API instead. ")
                } ~
                // get property
                get {
                  complete(
                    store
                      .value[StreamApp](id)
                      .map(
                        data =>
                          StreamPropertyResponse(id,
                                                 data.jarInfo.name,
                                                 data.name,
                                                 data.fromTopics,
                                                 data.toTopics,
                                                 data.instances,
                                                 data.lastModified)))
                } ~
                // update
                put {
                  entity(as[StreamPropertyRequest]) { req =>
                    complete(
                      store
                        .value[StreamApp](id)
                        .flatMap { data =>
                          val newData = toStore(
                            pipelineId = data.pipelineId,
                            streamId = data.id,
                            name = req.name,
                            instances = req.instances,
                            jarInfo = data.jarInfo,
                            fromTopics = req.fromTopics,
                            toTopics = req.toTopics,
                            lastModified = CommonUtils.current()
                          )
                          store.update[StreamApp](
                            id,
                            _ => Future.successful(newData)
                          )
                        }
                        .map(
                          newData =>
                            StreamPropertyResponse(id,
                                                   newData.jarInfo.name,
                                                   newData.name,
                                                   newData.fromTopics,
                                                   newData.toTopics,
                                                   newData.instances,
                                                   newData.lastModified)))
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
              complete(
                store.value[StreamApp](id).flatMap {
                  data =>
                    if (!assertParameters(data)) {
                      log.error(s"input data : ${data.toString}")
                      throw new IllegalArgumentException(
                        s"StreamApp with id : ${data.id} not match the parameter requirement."
                      )
                    }
                    // for v0.3, we only run streamApp with the following conditions:
                    // 1) use any available node of worker cluster to run streamApp
                    // 2) only support one from/to pair topic

                    // get the broker info and topic info from worker cluster
                    store
                      .value[Pipeline](data.pipelineId)
                      .flatMap { pipeline =>
                        CollieUtils.topicAdmin(Some(pipeline.workerClusterName))
                      }
                      .flatMap {
                        case (bkInfo, topicAdmin) =>
                          // get the brokerClusterName from the desire topics
                          store
                            .values[TopicInfo]
                            .map { topics =>
                              topics
                              // filter out not exists topics
                                .filter(t => {
                                  data.fromTopics.contains(t.id) || data.toTopics.contains(t.id)
                                })
                                .map(_.brokerClusterName)
                                //there should be only one possible broker cluster contains the desire topics
                                .headOption
                                .getOrElse(
                                  throw new RuntimeException(
                                    "Can not find andy match broker cluster for the required topics"
                                  )
                                )
                            }
                            // get the broker connection props
                            .map { bkName =>
                              if (bkInfo.name != bkName)
                                throw new RuntimeException(
                                  s"the broker name not match : from wk : ${bkInfo.name}, from bk : $bkName"
                                )
                              topicAdmin.connectionProps
                            }
                      }
                      .map { bkProps =>
                        jarStore
                          .url(data.jarInfo.id)
                          .flatMap {
                            val streamAppId = StreamApi.formatAppId(data.id)
                            url =>
                              crane
                                .streamWarehouse()
                                .creator()
                                .clusterName(data.id)
                                .instance(data.instances)
                                .imageName(STREAMAPP_IMAGE)
                                .jarUrl(url.toString)
                                .appId(streamAppId)
                                .brokerProps(bkProps)
                                .fromTopics(data.fromTopics)
                                .toTopics(data.toTopics)
                                .create()
                                .map { _ =>
                                  StreamActionResponse(
                                    id,
                                    Some(ContainerState.RUNNING.name)
                                  )
                                }
                          }
                          .recover {
                            case ex: Throwable =>
                              log.error(ex.getMessage)
                              StreamActionResponse(id, Some(ContainerState.EXITED.name))
                          }
                          .map { res =>
                            store.update[StreamApp](
                              id,
                              d =>
                                Future.successful(
                                  d.copy(
                                    state = res.state
                                  )
                              )
                            )
                            res
                          }
                      }
                }
              )
            }
          } ~
          // stop streamApp
          path(STOP_COMMAND) {
            put {
              complete(store.value[StreamApp](id).flatMap { _ =>
                crane.remove(id).map { _ =>
                  store.update[StreamApp](
                    id,
                    d =>
                      Future.successful(
                        d.copy(
                          state = None
                        )
                    )
                  )
                  StreamActionResponse(
                    id,
                    None
                  )
                }
              })
            }
          }
      }
    }
}
