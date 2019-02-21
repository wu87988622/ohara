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
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap, TimeUnit}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.agent.{BrokerCollie, DockerClient, NodeCollie}
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.StreamApi._
import com.island.ohara.client.configurator.v0.{ContainerApi, JarApi, StreamApi}
import com.island.ohara.common.util.{CommonUtil, Releasable}
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.jar.JarStore
import com.island.ohara.configurator.route.RouteUtil._
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.sys.process._
import scala.util.Random

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
    StreamApp(pipelineId, streamId, name, instances, jarInfo, fromTopics, toTopics, lastModified)

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

  private[this] val clientCache: DockerClientCache = new DockerClientCache {
    private[this] val cache: ConcurrentMap[Node, DockerClient] = new ConcurrentHashMap[Node, DockerClient]()
    override def get(node: Node): DockerClient = cache.computeIfAbsent(
      node,
      node =>
        DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build())

    override def close(): Unit = {
      cache.values().forEach(client => Releasable.close(client))
      cache.clear()
    }
  }
  private trait DockerClientCache extends Releasable {
    def get(node: Node): DockerClient
    def get(nodes: Seq[Node]): Seq[DockerClient] = nodes.map(get)
  }

  def apply(implicit store: Store,
            brokerCollie: BrokerCollie,
            nodeCollie: NodeCollie,
            jarStore: JarStore): server.Route =
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
                  onSuccess(
                    Future.sequence(files.map {
                      case (metadata, file) =>
                        if (file.length() > StreamApi.MAX_FILE_SIZE) {
                          throw new RuntimeException(
                            s"the file : ${metadata.fileName} size is bigger than ${StreamApi.MAX_FILE_SIZE / 1024 / 1024} MB.")
                        }
                        jarStore.add(file, s"${metadata.fileName}")
                    })
                  ) { jarInfos =>
                    val jars = jarInfos.map { jarInfo =>
                      val time = CommonUtil.current()
                      val streamId = CommonUtil.uuid()
                      store.add(toStore(pipelineId = id, streamId = streamId, jarInfo = jarInfo, lastModified = time))
                      StreamListResponse(streamId, jarInfo.name, time)
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
                  //TODO : check streamApp is not at running state...by Sam
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
                    val f = new File(StreamApi.TMP_ROOT, CommonUtil.randomString(5))
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
              //add property is impossible, we need streamApp id first
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
                      val newData = toStore(
                        pipelineId = data.pipelineId,
                        streamId = data.id,
                        name = req.name,
                        instances = req.instances,
                        jarInfo = data.jarInfo,
                        fromTopics = req.fromTopics,
                        toTopics = req.toTopics,
                        lastModified = CommonUtil.current()
                      )
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
                  if (!assertParameters(data))
                    throw new IllegalArgumentException(
                      s"StreamApp with id : ${data.id} not match the parameter requirement.")

                  // for v0.2, we only run streamApp with the following conditions:
                  // 1) use any available node to run streamApp
                  // 2) only support one from/to topic
                  // 3) choose any existing broker cluster to run
                  // 4) only run 1 instance
                  val result = for {
                    dockerClient <- nodeCollie
                      .nodes()
                      .map { nodes =>
                        Random
                          .shuffle(nodes)
                          .take(data.instances)
                          //we only accept 1 instance for 0.2...by Sam
                          .headOption
                          .getOrElse(throw new IllegalArgumentException(s"there is no any available node"))
                      }
                      .map { n =>
                        store.update[StreamApp](id, data => Future.successful(data.copy(node = Some(n))))
                        clientCache.get(n)
                      }
                    brokerInfos <- brokerCollie
                      .clusters()
                      .map { bkMap =>
                        bkMap.keys.toSeq
                      }
                      .map { clusters =>
                        clusters.filter { cluster =>
                          Await
                            .result(
                              CollieUtils
                                .topicAdmin(Some(cluster.name))
                                .flatMap {
                                  case (_, client) =>
                                    client.list()
                                }
                                .filter { infos =>
                                  infos.map(info => info.name).contains(data.fromTopics.head) &&
                                  infos.map(info => info.name).contains(data.toTopics.head)
                                },
                              Duration(10, TimeUnit.SECONDS)
                            )
                            .nonEmpty
                        }
                      }
                    //there should be only one possible broker cluster
                  } yield (dockerClient -> brokerInfos.headOption)

                  result.map {
                    case (client, brokerInfo) =>
                      if (brokerInfo.isEmpty) {
                        throw new RuntimeException("Can not find andy math broker cluster for this streamApp")
                      }
                      val brokers = brokerInfo.get.nodeNames.map(_ + ":9092").mkString(",")
                      jarStore
                        .url(data.jarInfo.id)
                        .map {
                          val appId = StreamApi.formatAppId(data.id)
                          url =>
                            client
                              .container(appId)
                              .getOrElse(
                                client
                                  .containerCreator()
                                  .name(appId)
                                  .envs(
                                    Map(
                                      "STREAMAPP_JARURL" -> url.toString,
                                      "STREAMAPP_APPID" -> appId,
                                      "STREAMAPP_SERVERS" -> brokers,
                                      "STREAMAPP_FROMTOPIC" -> data.fromTopics.head,
                                      "STREAMAPP_TOTOPIC" -> data.toTopics.head
                                    )
                                  )
                                  .imageName(StreamApi.STREAMAPP_IMAGE)
                                  .command(StreamApi.MAIN_ENTRY)
                                  .run()
                                  .get
                              )
                        }
                        .map { c =>
                          log.info(s"container [${c.name}] logs : s${client.log(c.name)}")
                          StreamActionResponse(
                            id,
                            Some(c.state)
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
                }
              )(complete(_))
            }
          } ~
          // stop streamApp
          path(STOP_COMMAND) {
            put {
              onSuccess(store.value[StreamApp](id).flatMap { data =>
                val node = data.node.getOrElse(
                  throw new RuntimeException(s"the streamApp [$id] is not running at any node. we cannot stop it."))
                Future
                  .successful(clientCache.get(node).forceRemove(StreamApi.formatAppId(data.id)))
                  .map { _ =>
                    StreamActionResponse(
                      id,
                      None
                    )
                  }
                  .recover {
                    case _: Throwable =>
                      StreamActionResponse(
                        id,
                        Some(ContainerApi.ContainerState.EXITED)
                      )
                  }
              })(complete(_))
            }
          }
      }
    }
}
