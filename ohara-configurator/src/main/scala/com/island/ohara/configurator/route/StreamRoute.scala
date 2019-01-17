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
import com.island.ohara.client.configurator.v0.JarApi
import com.island.ohara.client.configurator.v0.StreamApi._
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.jar.JarStore
import com.island.ohara.configurator.route.RouteUtil._
import com.island.ohara.kafka.BrokerClient
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.sys.process._

private[configurator] object StreamRoute {

  private[this] def toStore(pipelineId: String,
                            jarId: String,
                            jarInfo: JarApi.JarInfo,
                            lastModified: Long): StreamData =
    StreamData(pipelineId, jarId, "", 1, jarInfo, Seq.empty, Seq.empty, lastModified)

  private[this] def toStore(pipelineId: String,
                            jarId: String,
                            appId: String,
                            instances: Int,
                            jarInfo: JarApi.JarInfo,
                            fromTopics: Seq[String],
                            toTopics: Seq[String],
                            lastModified: Long): StreamData =
    StreamData(pipelineId, jarId, appId, instances, jarInfo, fromTopics, toTopics, lastModified)

  private[this] def assertParameters(data: StreamData): Boolean = {
    data.pipelineId.nonEmpty &&
    data.id.nonEmpty &&
    data.name.nonEmpty &&
    data.instances >= 1 &&
    data.fromTopics.nonEmpty &&
    data.toTopics.nonEmpty
  }

  def apply(implicit store: Store, brokerClient: BrokerClient, jarStore: JarStore): server.Route =
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
                    complete(jars)
                  }
                }
              } ~
                //return list
                get {
                  onSuccess(store.values[StreamData]) { values =>
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
                    f1 <- store.remove[StreamData](id)
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
                      f1 <- store.value[StreamData](id)
                      f2 <- jarStore.url(f1.jarInfo.id)
                      f3 <- {
                        { f2 #> f !! }
                        jarStore.add(f, req.jarName)
                      }
                      f4 <- jarStore.remove(f1.jarInfo.id)
                      f5 <- store.update[StreamData](
                        id,
                        previous =>
                          toStore(previous.pipelineId,
                                  previous.id,
                                  previous.name,
                                  previous.instances,
                                  f3,
                                  previous.fromTopics,
                                  previous.toTopics,
                                  CommonUtil.current())
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
                  onSuccess(store.value[StreamData](id)) { data =>
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
                    onSuccess(
                      store.update[StreamData](
                        id,
                        oldData =>
                          toStore(oldData.pipelineId,
                                  oldData.id,
                                  req.name,
                                  req.instances,
                                  oldData.jarInfo,
                                  req.fromTopics,
                                  req.toTopics,
                                  CommonUtil.current())
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
          // start streamapp
          path(START_COMMAND) {
            put {
              onSuccess(store.value[StreamData](id)) { data =>
                if (!assertParameters(data))
                  throw new IllegalArgumentException(
                    s"StreamData with id : ${data.id} not match the parameter requirement.")
                val checkDocker = "which docker" !!

                if (checkDocker.toLowerCase.contains("not found"))
                  throw new RuntimeException(s"This machine is not support docker command !")

                //TODO : we hard code here currently. This should be called from agent ...by Sam
                val dockerCmd =
                  s"""docker run -d -h "${data.name}" -v /home/docker/streamapp:/opt/ohara/streamapp --rm --name "${data.name}"
                     | -e STREAMAPP_SERVERS=${brokerClient.brokers()}
                     | -e STREAMAPP_APPID=${data.name}
                     | -e STREAMAPP_FROMTOPIC=${data.fromTopics.head}
                     | -e STREAMAPP_TOTOPIC=${data.toTopics.head}
                     | ${StreamClient.STREAMAPP_IMAGE}
                     | "example.MyApp"
                          """.stripMargin

                // TODO: use LOG instead...by Sam
                System.out.println(s"command : $dockerCmd")
                complete(if (Process(dockerCmd).run.exitValue() == 0) StatusCodes.OK else StatusCodes.BadRequest)
              }
            }
          } ~
          // stop streamapp
          path(STOP_COMMAND) {
            put {
              onSuccess(store.value[StreamData](id)) { data =>
                val checkDocker = "which docker" !!

                if (checkDocker.toLowerCase.contains("not found"))
                  throw new RuntimeException(s"This machine is not support docker command !")

                //TODO : we hard code here currently. This should be called by agent ...by Sam
                val dockerCmd =
                  s"""docker stop ${data.name}
               """.stripMargin
                complete(if (Process(dockerCmd).run.exitValue() == 0) StatusCodes.OK else StatusCodes.BadRequest)
              }
            }
          }
      }
    }
}
