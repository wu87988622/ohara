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
import com.island.ohara.client.StreamClient
import com.island.ohara.client.configurator.v0.StreamApi._
import com.island.ohara.client.configurator.v0.{ContainerApi, JarApi}
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.jar.JarStore
import com.island.ohara.configurator.route.RouteUtil._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.sys.process._

private[configurator] object StreamRoute {

  private[this] def toStore(pipelineId: String, jarId: String, jarInfo: JarApi.JarInfo, lastModified: Long): StreamApp =
    StreamApp(pipelineId, jarId, "", 1, jarInfo, Seq.empty, Seq.empty, lastModified)

  private[this] def toStore(pipelineId: String,
                            jarId: String,
                            appId: String,
                            instances: Int,
                            jarInfo: JarApi.JarInfo,
                            fromTopics: Seq[String],
                            toTopics: Seq[String],
                            lastModified: Long): StreamApp =
    StreamApp(pipelineId, jarId, appId, instances, jarInfo, fromTopics, toTopics, lastModified)

  private[this] def assertParameters(data: StreamApp): Boolean = {
    data.pipelineId.nonEmpty &&
    data.id.nonEmpty &&
    data.name.nonEmpty &&
    data.instances >= 1 &&
    data.fromTopics.nonEmpty &&
    data.toTopics.nonEmpty
  }

  def apply(implicit store: Store, jarStore: JarStore): server.Route =
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
                storeUploadedFiles(StreamClient.INPUT_KEY, StreamClient.saveTmpFile) { files =>
                  onSuccess(
                    Future.sequence(files.map {
                      case (metadata, file) =>
                        if (file.length() > StreamClient.MAX_FILE_SIZE) {
                          throw new RuntimeException(
                            s"the file : ${metadata.fileName} size is bigger than ${StreamClient.MAX_FILE_SIZE / 1024 / 1024} MB.")
                        }
                        jarStore.add(file, s"${metadata.fileName}")
                    })
                  ) { jarInfos =>
                    val jars = jarInfos.map { jarInfo =>
                      val time = CommonUtil.current()
                      val jarId = CommonUtil.uuid()
                      store.add(toStore(id, jarId, jarInfo, time))
                      StreamListResponse(jarId, jarInfo.name, time)
                    }
                    //delete temp jars after success
                    files.foreach { case (_, file) => file.deleteOnExit() }
                    complete(jars)
                  }
                }
              } ~
                //return list
                get {
                  onSuccess(store.values[StreamApp]) { values =>
                    complete(values
                      .filter(f => f.pipelineId.equals(id)) //note : this id is given by UI (pipeline_id)
                      .map(data => StreamListResponse(data.id, data.jarInfo.name, data.lastModified)))
                  }
                } ~
                //delete jar
                delete {
                  //TODO : check streamapp is not at running state...by Sam
                  assertNotRelated2Pipeline(id)
                  val result = for {
                    f1 <- store.remove[StreamApp](id)
                    f2 <- jarStore.remove(f1.jarInfo.id)
                  } yield f1

                  onSuccess(result) { data =>
                    complete(StreamListResponse(data.id, data.jarInfo.name, data.lastModified))
                  }
                } ~
                //update jar name
                put {
                  entity(as[StreamListRequest]) { req =>
                    if (req.jarName == null || req.jarName.isEmpty)
                      throw new IllegalArgumentException(s"Require jarName")
                    val f = new File(StreamClient.TMP_ROOT, CommonUtil.randomString(5))
                    val result = for {
                      f1 <- store.value[StreamApp](id)
                      f2 <- jarStore.url(f1.jarInfo.id)
                      f3 <- {
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
                                    CommonUtil.current()))
                      )
                    } yield f5
                    onSuccess(result) { newData =>
                      complete(StreamListResponse(newData.id, newData.jarInfo.name, newData.lastModified))
                    }
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
              //add property is impossible, we need streamapp id first
              post { complete("unsupported method") } ~
                // delete property is useless, we handle this in StreamApp List -> DELETE method
                delete { complete("unsupported method") } ~
                // get property
                get {
                  onSuccess(store.value[StreamApp](id)) { data =>
                    complete(
                      StreamPropertyResponse(id,
                                             data.jarInfo.name,
                                             data.name,
                                             data.fromTopics,
                                             data.toTopics,
                                             data.instances,
                                             data.lastModified))
                  }
                } ~
                // update
                put {
                  entity(as[StreamPropertyRequest]) { req =>
                    if (req.instances < 1)
                      throw new IllegalArgumentException(s"Require instances bigger or equal to 1")
                    onSuccess(store.update[StreamApp](
                      id,
                      oldData =>
                        Future.successful(
                          toStore(oldData.pipelineId,
                                  oldData.id,
                                  req.name,
                                  req.instances,
                                  oldData.jarInfo,
                                  req.fromTopics,
                                  req.toTopics,
                                  CommonUtil.current()))
                    )) { newData =>
                      complete(
                        StreamPropertyResponse(id,
                                               newData.jarInfo.name,
                                               newData.name,
                                               newData.fromTopics,
                                               newData.toTopics,
                                               newData.instances,
                                               newData.lastModified))
                    }
                  }
                }
            }
        } ~ pathPrefix(Segment) { id =>
        pathEnd {
          complete(StatusCodes.BadRequest -> "wrong uri")
        } ~
          //TODO : implement start action...by Sam
          // start streamApp
          path(START_COMMAND) {
            put {
              onSuccess(store.value[StreamApp](id)) { data =>
                if (!assertParameters(data))
                  throw new IllegalArgumentException(
                    s"StreamData with id : ${data.id} not match the parameter requirement.")
                complete(StreamActionResponse(id, Some(ContainerApi.ContainerState.RUNNING)))
              }
            }
          } ~
          //TODO : implement stop action...by Sam
          // stop streamApp
          path(STOP_COMMAND) {
            put {
              onSuccess(store.value[StreamApp](id)) { data =>
                if (!assertParameters(data))
                  throw new IllegalArgumentException(
                    s"StreamData with id : ${data.id} not match the parameter requirement.")
                complete(StreamActionResponse(id, None))
              }
            }
          }
      }
    }
}
