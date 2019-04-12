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

package com.island.ohara.agent.k8s

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.island.ohara.agent.k8s.K8SClient.ContainerCreator
import com.island.ohara.agent.k8s.K8SJson.{
  Container,
  CreatePod,
  CreatePodContainer,
  CreatePodEnv,
  CreatePodLabel,
  CreatePodMetadata,
  CreatePodNodeSelector,
  CreatePodPortMapping,
  CreatePodResult,
  CreatePodSpec,
  HostAliases,
  K8SErrorResponse,
  K8SNodeInfo,
  K8SPodInfo,
  NodeItems
}
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, PortMapping, PortPair}
import com.island.ohara.common.util.{CommonUtils, ReleaseOnce}
import com.typesafe.scalalogging.Logger
import spray.json.{RootJsonFormat, _}

import scala.concurrent.{Await, ExecutionContext, Future}

case class K8SStatusInfo(isHealth: Boolean, message: String)
case class Report(nodeName: String, isK8SNode: Boolean, statusInfo: Option[K8SStatusInfo])

trait K8SClient extends ReleaseOnce {
  def containers(implicit executionContext: ExecutionContext): Seq[ContainerInfo]
  def remove(name: String)(implicit executionContext: ExecutionContext): ContainerInfo
  def removeNode(clusterName: String, nodeName: String, serviceName: String)(
    implicit executionContext: ExecutionContext): Seq[ContainerInfo]
  def log(name: String): String
  def nodeNameIPInfo(implicit executionContext: ExecutionContext): Seq[HostAliases]
  def containerCreator(): ContainerCreator
  def images(nodeName: String)(implicit executionContext: ExecutionContext): Future[Seq[String]]
  def checkNode(nodeName: String)(implicit executionContext: ExecutionContext): Future[Report]
}

object K8SClient {
  private val LOG = Logger(classOf[K8SClient])

  import scala.concurrent.duration._
  val TIMEOUT: FiniteDuration = 30 seconds

  private[agent] val K8S_KIND_NAME = "K8S"

  def apply(k8sApiServerURL: String): K8SClient = {
    if (k8sApiServerURL.isEmpty) throw new IllegalArgumentException(s"invalid kubernetes api:${k8sApiServerURL}")

    new K8SClient() with SprayJsonSupport {
      private[this] implicit val actorSystem: ActorSystem = ActorSystem(s"${classOf[K8SClient].getSimpleName}")
      private[this] implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

      override def containers(implicit executionContext: ExecutionContext): Seq[ContainerInfo] = {
        val k8sPodInfo: K8SPodInfo = Await.result(
          Http()
            .singleRequest(HttpRequest(HttpMethods.GET, uri = s"${k8sApiServerURL}/pods"))
            .flatMap(unmarshal[K8SPodInfo]),
          TIMEOUT
        )

        k8sPodInfo.items.map(item => {
          val containerInfo: Container = item.spec.containers.head
          val phase = item.status.phase
          val hostIP = item.status.hostIP

          ContainerInfo(
            item.spec.nodeName.getOrElse("Unknown"),
            item.metadata.uid,
            containerInfo.image,
            item.metadata.creationTimestamp,
            K8sContainerState.all
              .find(s => phase.toLowerCase().contains(s.name.toLowerCase))
              .getOrElse(K8sContainerState.UNKNOWN)
              .name,
            K8S_KIND_NAME,
            item.spec.hostname.getOrElse("Unknown"),
            "Unknown",
            Seq(
              PortMapping(
                hostIP.getOrElse("Unknown"),
                containerInfo.ports.getOrElse(Seq()).map(x => PortPair(x.hostPort.getOrElse(0), x.containerPort)))),
            containerInfo.env.getOrElse(Seq()).map(x => (x.name -> x.value.getOrElse(""))).toMap,
            item.spec.hostname.getOrElse("")
          )
        })
      }

      override def images(nodeName: String)(implicit executionContext: ExecutionContext): Future[Seq[String]] =
        Http().singleRequest(HttpRequest(HttpMethods.GET, uri = s"${k8sApiServerURL}/nodes/${nodeName}")).flatMap {
          response =>
            Unmarshal(response.entity).to[NodeItems] map { r =>
              r.status.images.filter(x => x.names.size >= 2).map(x => x.names.last)
            }
        }

      override def checkNode(nodeName: String)(implicit executionContext: ExecutionContext): Future[Report] = {
        Http().singleRequest(HttpRequest(HttpMethods.GET, uri = s"${k8sApiServerURL}/nodes")).flatMap { response =>
          Unmarshal(response.entity).to[K8SNodeInfo] map { r =>
            val filterNode: Seq[NodeItems] = r.items.filter(x => x.metadata.name.equals(nodeName))
            val isK8SNode: Boolean = filterNode.size == 1
            var statusInfo: Option[K8SStatusInfo] = None
            if (isK8SNode)
              statusInfo = Some(
                filterNode
                  .flatMap(x => {
                    x.status.conditions.filter(y => y.conditionType.equals("Ready"))
                  }.map(x => {
                    if (x.status.equals("True")) K8SStatusInfo(true, x.message)
                    else K8SStatusInfo(false, x.message)
                  }))
                  .head)
            Report(nodeName, isK8SNode, statusInfo)
          }
        }
      }

      override def remove(name: String)(implicit executionContext: ExecutionContext): ContainerInfo = {
        containers
          .find(_.name == name)
          .map(container => {
            LOG.info(s"Container ${container.name} is removing")

            Await.result(
              Http().singleRequest(
                HttpRequest(HttpMethods.DELETE, uri = s"${k8sApiServerURL}/namespaces/default/pods/${container.name}")),
              TIMEOUT
            )
            container
          })
          .getOrElse(throw new IllegalArgumentException(s"Name:$name doesn't exist"))
      }

      override def removeNode(clusterName: String, nodeName: String, serviceName: String)(
        implicit executionContext: ExecutionContext): Seq[ContainerInfo] = {
        val key = s"$clusterName${K8SClusterCollieImpl.DIVIDER}${serviceName}"
        val removeContainer: Seq[ContainerInfo] = containers
          .filter(c => c.name.startsWith(key) && c.nodeName.equals(nodeName))
          .map(container => {
            Await.result(
              Http().singleRequest(
                HttpRequest(HttpMethods.DELETE, uri = s"${k8sApiServerURL}/namespaces/default/pods/${container.name}")),
              TIMEOUT
            )
            container
          })

        var isRemovedContainer: Boolean = false
        while (!isRemovedContainer) {
          if (containers.filter(c => c.name.startsWith(key) && c.nodeName.equals(nodeName)).size == 0) {
            isRemovedContainer = true
          }
        }
        removeContainer
      }

      override def log(name: String): String = {
        val log: Future[String] = Unmarshal(
          Await
            .result(Http().singleRequest(
                      HttpRequest(HttpMethods.GET, uri = s"${k8sApiServerURL}/namespaces/default/pods/${name}/log")),
                    TIMEOUT)
            .entity).to[String]
        Option(Await.result(log, TIMEOUT))
          .map(msg => if (msg.contains("ERROR:")) throw new IllegalArgumentException(msg) else msg)
          .getOrElse(throw new IllegalArgumentException(s"no response from $name contains"))
      }

      override def nodeNameIPInfo(implicit executionContext: ExecutionContext): Seq[HostAliases] = {
        val k8sNodeInfo: K8SNodeInfo = Await.result(
          Http()
            .singleRequest(HttpRequest(HttpMethods.GET, uri = s"${k8sApiServerURL}/nodes"))
            .flatMap(unmarshal[K8SNodeInfo]),
          TIMEOUT
        )

        k8sNodeInfo.items.map(item => {
          val internalIP: String =
            item.status.addresses.filter(node => node.nodeType.equals("InternalIP")).head.nodeAddress
          val hostName: String = item.status.addresses.filter(node => node.nodeType.equals("Hostname")).head.nodeAddress
          HostAliases(internalIP, Seq(hostName))
        })

      }

      override def containerCreator(): ContainerCreator = new ContainerCreator() {
        private[this] var name: String = CommonUtils.randomString()
        private[this] var imageName: String = _
        private[this] var hostname: String = _
        private[this] var nodename: String = _
        private[this] var domainName: String = _
        private[this] var labelName: String = _
        private[this] var envs: Map[String, String] = Map.empty
        private[this] var ports: Map[Int, Int] = Map.empty

        override def name(name: String): ContainerCreator = {
          this.name = name
          this
        }

        override def imageName(imageName: String): ContainerCreator = {
          this.imageName = imageName
          this
        }

        override def hostname(hostname: String): ContainerCreator = {
          this.hostname = hostname
          this
        }

        override def envs(envs: Map[String, String]): ContainerCreator = {
          this.envs = envs
          this
        }

        override def portMappings(ports: Map[Int, Int]): ContainerCreator = {
          this.ports = ports
          this
        }

        override def nodename(nodename: String): ContainerCreator = {
          this.nodename = nodename
          this
        }

        override def domainName(domainName: String): ContainerCreator = {
          this.domainName = domainName
          this
        }

        override def labelName(labelName: String): ContainerCreator = {
          this.labelName = labelName
          this
        }

        override def run()(implicit executionContext: ExecutionContext): Option[ContainerInfo] = {
          val podSpec = CreatePodSpec(
            CreatePodNodeSelector(nodename),
            hostname,
            domainName,
            nodeNameIPInfo,
            Seq(
              CreatePodContainer(labelName,
                                 imageName,
                                 envs.map(x => CreatePodEnv(x._1, x._2)).toSeq,
                                 ports.map(x => CreatePodPortMapping(x._1, x._2)).toSeq))
          )

          val requestJson =
            CreatePod("v1", "Pod", CreatePodMetadata(hostname, CreatePodLabel(labelName)), podSpec).toJson.toString
          LOG.info(s"create pod request json: ${requestJson}")

          val createPodInfo = Await.result(
            Http()
              .singleRequest(
                HttpRequest(HttpMethods.POST,
                            entity = HttpEntity(ContentTypes.`application/json`, requestJson),
                            uri = s"${k8sApiServerURL}/namespaces/default/pods"))
              .flatMap(unmarshal[CreatePodResult]),
            TIMEOUT
          )

          Option(
            ContainerInfo(
              nodename,
              createPodInfo.metadata.uid,
              imageName,
              createPodInfo.metadata.creationTimestamp,
              K8sContainerState.all
                .find(s => createPodInfo.status.phase.toLowerCase.contains(s.name.toLowerCase))
                .getOrElse(K8sContainerState.UNKNOWN)
                .name,
              K8S_KIND_NAME,
              createPodInfo.metadata.name,
              "Unknown",
              ports.map(x => PortMapping(hostname, Seq(PortPair(x._1, x._2)))).toSeq,
              envs,
              hostname
            ))
        }
      }

      override protected def doClose(): Unit = {
        Await.result(actorSystem.terminate(), 60 seconds)
      }

      private[this] def unmarshal[T](response: HttpResponse)(implicit um: RootJsonFormat[T],
                                                             executionContext: ExecutionContext): Future[T] =
        if (response.status.isSuccess()) Unmarshal(response).to[T]
        else
          Unmarshal(response)
            .to[K8SErrorResponse]
            .flatMap(error => {
              Future.failed(new RuntimeException(error.message))
            })
    }
  }

  trait ContainerCreator {
    def name(name: String): ContainerCreator

    def imageName(imageName: String): ContainerCreator

    def hostname(hostname: String): ContainerCreator

    def envs(envs: Map[String, String]): ContainerCreator

    def portMappings(ports: Map[Int, Int]): ContainerCreator

    def nodename(nodename: String): ContainerCreator

    def run()(implicit executionContext: ExecutionContext): Option[ContainerInfo]

    def domainName(domainName: String): ContainerCreator

    def labelName(labelName: String): ContainerCreator
  }

}
