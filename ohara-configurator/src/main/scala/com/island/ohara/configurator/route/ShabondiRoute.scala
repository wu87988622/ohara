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

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.agent.k8s.K8SClient
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.ShabondiApi._
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.store.DataStore
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object ShabondiRoute {
  private[this] lazy val LOG = Logger(ShabondiRoute.getClass)

  private def addShabondi(store: DataStore)(implicit executionContext: ExecutionContext) = {
    val newShabondi =
      ShabondiDescription(CommonUtils.uuid(), "shabondi name", CommonUtils.current(), None, Seq.empty, -1, 1)

    store.add[ShabondiDescription](newShabondi)
  }

  private def getProperty(id: String, store: DataStore)(implicit executionContext: ExecutionContext) = {
    store.value[ShabondiDescription](id)
  }

  private def deleteShabondi(id: String, store: DataStore)(implicit executionContext: ExecutionContext) = {
    store.exist[ShabondiDescription](id).flatMap { isExist =>
      if (isExist) {
        store.remove[ShabondiDescription](id)
      } else
        throw new IllegalArgumentException(s"Shabondi not exist: $id")
    }
  }

  private def updateProperty(id: String, property: ShabondiProperty, store: DataStore)(
    implicit executionContext: ExecutionContext) = {
    LOG.info(s"update shabondi: $id")
    val updateValue = (data: ShabondiDescription) =>
      Future.successful(
        duplicateShabondiDescription(data, property).copy(lastModified = CommonUtils.current())
    )
    store.update[ShabondiDescription](id, updateValue)
  }

  private def updateShabondiState(id: String, state: String, store: DataStore)(
    implicit executionContext: ExecutionContext) = {
    LOG.info(s"update shabondi: $id")
    val updateValue = (data: ShabondiDescription) =>
      Future.successful(
        data.copy(state = Some(state), lastModified = CommonUtils.current())
    )
    store.update[ShabondiDescription](id, updateValue)
  }

  private def randomPickNode(store: DataStore)(implicit executionContext: ExecutionContext): Node = {
    val random = new scala.util.Random
    val nodes = awaitResult(store.values[Node])
    if (nodes.isEmpty)
      throw new RuntimeException("Cannot find any ohara node.")
    nodes(random.nextInt(nodes.length))
  }

  private def startShabondi(id: String, k8sClient: K8SClient, store: DataStore)(
    implicit executionContext: ExecutionContext) = {
    val nodeName = randomPickNode(store).name
    val podName = POD_NAME_PREFIX + id
    createContainer(k8sClient, nodeName, podName).flatMap {
      case Some(container) =>
        LOG.info(s"Shabondi pod created: $podName")
        updateShabondiState(id, container.state, store)
      case None =>
        throw new Exception("Shabondi starting fail...")
    }
  }

  private def stopShabondi(id: String, k8sClient: K8SClient, store: DataStore)(
    implicit executionContext: ExecutionContext) = {
    LOG.info(s"shabondi stop: $id")
    val podName = POD_NAME_PREFIX + id
    k8sClient.remove(podName).flatMap { container =>
      LOG.info(s"Shabondi pod removed: $podName")
      updateShabondiState(id, container.state, store)
    }
  }

  def apply(k8sClientOpt: Option[K8SClient])(implicit store: DataStore,
                                             executionContext: ExecutionContext): server.Route =
    pathPrefix(PATH_PREFIX) {
      pathEnd {
        post { complete { addShabondi(store) } }
      }
    } ~
      pathPrefix(PATH_PREFIX / Segment) { id: String =>
        pathEnd {
          get { complete { getProperty(id, store) } } ~
            put {
              entity(as[ShabondiProperty]) { prop: ShabondiProperty =>
                complete { updateProperty(id, prop, store) }
              }
            } ~
            delete { complete { deleteShabondi(id, store) } }
        } ~
          path(PATH_SEGMENT_START) {
            // TODO: need integrate with Crane
            k8sClientOpt match {
              case Some(k8sClient) =>
                put { complete { startShabondi(id, k8sClient, store) } }
              case None =>
                complete(StatusCodes.ServiceUnavailable -> "Shabondi need K8SClient...")
            }
          } ~
          path(PATH_SEGMENT_STOP) {
            // TODO: need integrate with Crane
            k8sClientOpt match {
              case Some(k8sClient) =>
                put { complete { stopShabondi(id, k8sClient, store) } }
              case None =>
                complete(StatusCodes.ServiceUnavailable -> "Shabondi need K8SClient...")
            }
          }
      }

  private def awaitResult[T](f: Future[T]): T = Await.result(f, 10 seconds)

  private val POD_LABEL = "shabondi"
  private val POD_DOMAIN_NAME = "default"
  private val POD_NAME_PREFIX = "shabondi-"
  private val POD_NAME = "shabondi-host"

  private def createContainer(k8sClient: K8SClient, slaveNode: String, podHostname: String)(
    implicit executionContext: ExecutionContext) = {
    val creator: K8SClient.ContainerCreator = awaitResult(k8sClient.containerCreator())
    creator
      .imageName(IMAGE_NAME_DEFAULT)
      .portMappings(
        Map(
          9090 -> 8080
        ))
      .nodename(slaveNode)
      .hostname(podHostname)
      .labelName(POD_LABEL)
      .domainName(POD_DOMAIN_NAME)
      .name(POD_NAME)
      .run()
  }

  // TODO: we need a general function to copy object with different type of object or another function...
  private def duplicateShabondiDescription(origin: ShabondiDescription, prop: ShabondiProperty): ShabondiDescription = {
    var duplicate = origin
    if (prop.name.isDefined) duplicate = duplicate.copy(name = prop.name.get)
    if (prop.to.isDefined) duplicate = duplicate.copy(to = prop.to.get)
    if (prop.port.isDefined) duplicate = duplicate.copy(port = prop.port.get)
    duplicate
  }

}
