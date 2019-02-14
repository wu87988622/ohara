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
import java.util.concurrent.TimeUnit

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.agent.BrokerCollie
import com.island.ohara.client.StreamClient
import com.island.ohara.client.configurator.v0.StreamApi._
import com.island.ohara.client.configurator.v0.{ContainerApi, JarApi}
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.jar.JarStore
import com.island.ohara.configurator.route.RouteUtil._
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.sys.process._

private[configurator] object StreamRoute {

  private[this] val log = LoggerFactory.getLogger(StreamRoute.getClass)
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
    def isNotNullOrEmpty(str: String): Boolean = { str != null && str.nonEmpty }
    data.pipelineId.nonEmpty &&
    data.id.nonEmpty &&
    data.name.nonEmpty &&
    // TODO : we only support 1 instance for v0.2...by Sam
    data.instances == 1 &&
    data.fromTopics.nonEmpty &&
    data.fromTopics.forall(isNotNullOrEmpty) &&
    data.toTopics.nonEmpty &&
    data.toTopics.forall(isNotNullOrEmpty)
  }

  def apply(implicit store: Store, brokerCollie: BrokerCollie, jarStore: JarStore): server.Route =
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
                    onSuccess(store.value[StreamApp](id).flatMap { data =>
                      val newData = toStore(data.pipelineId,
                                            data.id,
                                            req.name,
                                            req.instances,
                                            data.jarInfo,
                                            req.fromTopics,
                                            req.toTopics,
                                            CommonUtil.current())
                      if (!assertParameters(newData))
                        throw new IllegalArgumentException(
                          s"StreamApp with id : ${data.id} not match the parameter requirement.")
                      store.update[StreamApp](
                        id,
                        _ => Future.successful(newData)
                      )
                    }) { newData =>
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
          // start streamApp
          path(START_COMMAND) {
            put {
              onSuccess(
                store.value[StreamApp](id).flatMap { data =>
                  // TODO : Use Collie as parameter to run streamApp instead ; need support multiple topics...by Sam
                  // for v0.2, we only run streamApp with the following conditions:
                  // 1) use "this configurator" machine as the docker client
                  // 2) only support one from/to topic
                  // 3) choose any existing broker cluster to run
                  // 4) only run 1 instance

                  val localPath = s"${StreamClient.TMP_ROOT}${File.separator}StreamApp-${data.name}"

                  val clusters = Await.result(brokerCollie.clusters().map { bkMap =>
                    bkMap.keys.toSeq
                  }, Duration(10, TimeUnit.SECONDS))

                  val brokers = clusters
                    .find { cluster =>
                      val topics = Await.result(
                        CollieUtils
                          .topicAdmin(Some(cluster.name))
                          .flatMap {
                            case (_, client) =>
                              client.list()
                          }
                          .map { infos =>
                            infos.map(info => info.name)
                          },
                        Duration(10, TimeUnit.SECONDS)
                      )
                      topics.contains(data.fromTopics.head) && topics.contains(data.toTopics.head)
                    }
                    .getOrElse(throw new NoSuchElementException(
                      "Can not find andy math broker cluster for this streamApp"))
                    .nodeNames
                    .map(_ + ":9092")
                    .mkString(",")

                  def dockerCmd(instance: Int): String = {
                    s"""docker run
                       | -h ${data.name}
                       | -d
                       | --name ${data.name}-$instance
                       | -e "STREAMAPP_JARROOT=/opt/ohara/streamapp"
                       | -e "STREAMAPP_FROMTOPIC=${data.fromTopics.head}"
                       | -e "STREAMAPP_APPID=${data.name}"
                       | -e "STREAMAPP_SERVERS=$brokers"
                       | -e "STREAMAPP_TOTOPIC=${data.toTopics.head}"
                       | -v "$localPath:/opt/ohara/streamapp"
                       | ${StreamClient.STREAMAPP_IMAGE}
                       | ${StreamClient.MAIN_ENTRY}""".stripMargin
                  }
                  def dockerLog(instance: Int): String =
                    s"docker logs ${data.name}-$instance"
                  def dockerInspectStatus(instance: Int): String =
                    s"docker inspect --format {{.State.Status}} ${data.name}-$instance"

                  val dir = new File(localPath)
                  if (!dir.exists()) dir.mkdirs()
                  val f = new File(localPath, data.jarInfo.name)

                  jarStore
                    .url(data.jarInfo.id)
                    .flatMap { url =>
                      //download the jar file from remote ftp server by URL...use more readable code ?...by Sam
                      Future { url #> f !! }
                    }
                    .flatMap { _ =>
                      Future {
                        (1 to data.instances).map(i => {
                          log.info(dockerCmd(i))
                          log.info(dockerLog(i))
                          Process(dockerCmd(i)).!(ProcessLogger(out => log.info(out), err => log.info(err)))
                          Process(dockerLog(i)).!(ProcessLogger(out => log.info(out), err => log.info(err)))
                        })
                      }
                    }
                    .map { _ =>
                      StreamActionResponse(
                        id,
                        Some(ContainerApi.ContainerState.all
                          .find(_.name.compareToIgnoreCase(dockerInspectStatus(1).lineStream.head) == 0)
                          .getOrElse(throw new IllegalArgumentException(s"Unknown state name")))
                      )
                    }
                    .recover {
                      case ex: Throwable =>
                        log.error(ex.getMessage)
                        StreamActionResponse(
                          id,
                          Some(ContainerApi.ContainerState.EXITED)
                        )
                    }
                }
              )(complete(_))
            }
          } ~
          // stop streamApp
          path(STOP_COMMAND) {
            put {
              onSuccess(store.value[StreamApp](id).flatMap { data =>
                // TODO : Use Collie as parameter to run streamApp instead ; need support multiple topics...by Sam
                def dockerCmd(instance: Int): String =
                  s"docker rm -f ${data.name}-$instance"

                for {
                  exitCode <- Future {
                    (1 to data.instances).map(i => {
                      log.info(dockerCmd(i))
                      Process(dockerCmd(i)).!(ProcessLogger(out => log.info(out), err => log.error(err)))
                    })
                  }
                  res <- Future {
                    StreamActionResponse(
                      id,
                      None
                    )
                  }.recover {
                    case _: Throwable =>
                      StreamActionResponse(
                        id,
                        Some(ContainerApi.ContainerState.EXITED)
                      )
                  }
                } yield res
              })(complete(_))
            }
          }
      }
    }
}
