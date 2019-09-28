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

import java.util.Objects

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
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
import com.island.ohara.client.{Enum, HttpExecutor}
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, PortMapping, PortPair}
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.CommonUtils

import scala.collection.JavaConverters._
import scala.concurrent.{Await, ExecutionContext, Future}

case class K8SStatusInfo(isHealth: Boolean, message: String)
case class Report(nodeName: String, isK8SNode: Boolean, statusInfo: Option[K8SStatusInfo])

trait K8SClient {
  def containers()(implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]]
  def remove(name: String)(implicit executionContext: ExecutionContext): Future[ContainerInfo]
  def forceRemove(name: String)(implicit executionContext: ExecutionContext): Future[ContainerInfo]
  def removeNode(containerName: String, nodeName: String, serviceName: String)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]]
  def log(name: String)(implicit executionContext: ExecutionContext): Future[String]
  def nodeNameIPInfo()(implicit executionContext: ExecutionContext): Future[Seq[HostAliases]]
  def containerCreator()(implicit executionContext: ExecutionContext): ContainerCreator
  def images(nodeName: String)(implicit executionContext: ExecutionContext): Future[Seq[String]]
  def checkNode(nodeName: String)(implicit executionContext: ExecutionContext): Future[Report]
}

object K8SClient {
  import scala.concurrent.duration._
  val TIMEOUT: FiniteDuration = 30 seconds

  private[agent] val K8S_KIND_NAME = "K8S"

  def apply(k8sApiServerURL: String): K8SClient = {
    if (k8sApiServerURL.isEmpty) throw new IllegalArgumentException(s"invalid kubernetes api:$k8sApiServerURL")

    new K8SClient() with SprayJsonSupport {
      override def containers()(implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
        HttpExecutor.SINGLETON
          .get[K8SPodInfo, K8SErrorResponse](s"$k8sApiServerURL/pods")
          .map(podInfo =>
            podInfo.items.map(item => {
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
                item.metadata.name,
                "Unknown",
                Seq(
                  PortMapping(
                    hostIP.getOrElse("Unknown"),
                    containerInfo.ports.getOrElse(Seq()).map(x => PortPair(x.hostPort.getOrElse(0), x.containerPort)))),
                containerInfo.env.getOrElse(Seq()).map(x => x.name -> x.value.getOrElse("")).toMap,
                item.spec.hostname.getOrElse("")
              )
            }))

      override def images(nodeName: String)(implicit executionContext: ExecutionContext): Future[Seq[String]] =
        HttpExecutor.SINGLETON
          .get[NodeItems, K8SErrorResponse](s"$k8sApiServerURL/nodes/$nodeName")
          .map(x => {
            x.status.images.flatMap(_.names)
          })

      override def checkNode(nodeName: String)(implicit executionContext: ExecutionContext): Future[Report] =
        HttpExecutor.SINGLETON.get[K8SNodeInfo, K8SErrorResponse](s"$k8sApiServerURL/nodes").map { r =>
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

      override def forceRemove(name: String)(implicit executionContext: ExecutionContext): Future[ContainerInfo] =
        removePod(name, true)

      override def remove(name: String)(implicit executionContext: ExecutionContext): Future[ContainerInfo] =
        removePod(name, false)

      override def removeNode(containerName: String, nodeName: String, serviceName: String)(
        implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] = {
        containers()
          .map(cs => cs.filter(c => c.name == containerName && c.nodeName == nodeName))
          .flatMap(
            cs =>
              Future.sequence(
                cs.map(container => {
                  remove(container.name)
                })
            ))
          .map(cs => {
            cs.map(container => {
              //Kubernetes remove pod container is async to need to await container remove completely to return the
              //container info.
              var isRemovedContainer: Boolean = false
              while (!isRemovedContainer) {
                if (!Await
                      .result(containers(), TIMEOUT)
                      .exists(c => c.name == containerName && c.nodeName == nodeName)) {
                  isRemovedContainer = true
                }
              }
              container
            })
          })
      }

      override def log(name: String)(implicit executionContext: ExecutionContext): Future[String] =
        HttpExecutor.SINGLETON
          .getOnlyMessage(s"$k8sApiServerURL/namespaces/default/pods/$name/log")
          .map(msg => if (msg.contains("ERROR:")) throw new IllegalArgumentException(msg) else msg)

      override def nodeNameIPInfo()(implicit executionContext: ExecutionContext): Future[Seq[HostAliases]] =
        HttpExecutor.SINGLETON
          .get[K8SNodeInfo, K8SErrorResponse](s"$k8sApiServerURL/nodes")
          .map(nodeInfo =>
            nodeInfo.items.map(item => {
              val internalIP: String =
                item.status.addresses.filter(node => node.nodeType.equals("InternalIP")).head.nodeAddress
              val hostName: String =
                item.status.addresses.filter(node => node.nodeType.equals("Hostname")).head.nodeAddress
              HostAliases(internalIP, Seq(hostName))
            }))

      override def containerCreator()(implicit executionContext: ExecutionContext): ContainerCreator =
        new ContainerCreator() {
          private[this] var name: String = CommonUtils.randomString()
          private[this] var imagePullPolicy: ImagePullPolicy = ImagePullPolicy.IFNOTPRESENT
          private[this] var restartPolicy: RestartPolicy = RestartPolicy.Never
          private[this] var imageName: String = _
          private[this] var hostname: String = _
          private[this] var nodeName: String = _
          private[this] var domainName: String = _
          private[this] var labelName: String = _
          private[this] var envs: Map[String, String] = Map.empty
          private[this] var ports: Map[Int, Int] = Map.empty
          private[this] var routes: Map[String, String] = Map.empty
          private[this] var command: Seq[String] = Seq.empty
          private[this] var args: Seq[String] = Seq.empty
          private[this] implicit var executionContext: ExecutionContext =
            scala.concurrent.ExecutionContext.Implicits.global

          override def name(name: String): ContainerCreator = {
            this.name = CommonUtils.requireNonEmpty(name)
            this
          }

          override def imageName(imageName: String): ContainerCreator = {
            this.imageName = CommonUtils.requireNonEmpty(imageName)
            this
          }

          override def hostname(hostname: String): ContainerCreator = {
            this.hostname = CommonUtils.requireNonEmpty(hostname)
            this
          }

          override def envs(envs: Map[String, String]): ContainerCreator = {
            this.envs = CommonUtils.requireNonEmpty(envs.asJava).asScala.toMap
            this
          }

          override def portMappings(ports: Map[Int, Int]): ContainerCreator = {
            this.ports = CommonUtils.requireNonEmpty(ports.asJava).asScala.toMap
            this
          }

          override def routes(routes: Map[String, String]): ContainerCreator = {
            this.routes = CommonUtils.requireNonEmpty(routes.asJava).asScala.toMap
            this
          }

          override def nodeName(nodeName: String): ContainerCreator = {
            this.nodeName = CommonUtils.requireNonEmpty(nodeName)
            this
          }

          override def domainName(domainName: String): ContainerCreator = {
            this.domainName = CommonUtils.requireNonEmpty(domainName)
            this
          }

          override def labelName(labelName: String): ContainerCreator = {
            this.labelName = CommonUtils.requireNonEmpty(labelName)
            this
          }

          @Optional
          override def pullImagePolicy(imagePullPolicy: ImagePullPolicy): ContainerCreator = {
            this.imagePullPolicy = Objects.requireNonNull(imagePullPolicy, "pullImagePolicy should not be null")
            this
          }

          @Optional("default is Never")
          override def restartPolicy(restartPolicy: RestartPolicy): ContainerCreator = {
            this.restartPolicy = Objects.requireNonNull(restartPolicy, "restartPolicy should not be null")
            this
          }

          @Optional("default is empty")
          override def command(command: Seq[String]): ContainerCreator = {
            this.command = CommonUtils.requireNonEmpty(command.asJava).asScala
            this
          }

          @Optional("default is empty")
          override def args(args: Seq[String]): ContainerCreator = {
            this.args = CommonUtils.requireNonEmpty(args.asJava).asScala
            this
          }

          override def threadPool(executionContext: ExecutionContext): ContainerCreator = {
            this.executionContext = Objects.requireNonNull(executionContext)
            this
          }

          override def create(): Future[Option[ContainerInfo]] =
            nodeNameIPInfo
              .map { ipInfo =>
                CreatePodSpec(
                  CreatePodNodeSelector(nodeName),
                  hostname, //hostname is container name
                  domainName,
                  ipInfo ++ routes.map { case (host, ip) => HostAliases(ip, Seq(host)) },
                  Seq(
                    CreatePodContainer(labelName,
                                       imageName,
                                       envs.map(x => CreatePodEnv(x._1, x._2)).toSeq,
                                       ports.map(x => CreatePodPortMapping(x._1, x._2)).toSeq,
                                       imagePullPolicy,
                                       command,
                                       args)),
                  restartPolicy
                )
              }
              .map(podSpec => { //name is pod name
                val request = CreatePod("v1", "Pod", CreatePodMetadata(name, CreatePodLabel(labelName)), podSpec)
                HttpExecutor.SINGLETON.post[CreatePod, CreatePodResult, K8SErrorResponse](
                  s"$k8sApiServerURL/namespaces/default/pods",
                  request)
              })
              .flatMap(
                _.map(createPodResult => {
                  Option(
                    ContainerInfo(
                      nodeName,
                      createPodResult.metadata.uid,
                      imageName,
                      createPodResult.metadata.creationTimestamp,
                      K8sContainerState.all
                        .find(s => createPodResult.status.phase.toLowerCase.contains(s.name.toLowerCase))
                        .getOrElse(K8sContainerState.UNKNOWN)
                        .name,
                      K8S_KIND_NAME,
                      createPodResult.metadata.name,
                      "Unknown",
                      ports.map(x => PortMapping(hostname, Seq(PortPair(x._1, x._2)))).toSeq,
                      envs,
                      hostname
                    ))
                })
              )

        }

      private[this] def removePod(name: String, isForce: Boolean)(
        implicit executionContext: ExecutionContext): Future[ContainerInfo] =
        containers()
          .map(_.find(_.name == name).getOrElse(throw new IllegalArgumentException(s"Name:$name doesn't exist")))
          .flatMap(container => {
            if (isForce)
              HttpExecutor.SINGLETON.delete[K8SErrorResponse](
                s"$k8sApiServerURL/namespaces/default/pods/${container.name}?gracePeriodSeconds=0")
            else
              HttpExecutor.SINGLETON
                .delete[K8SErrorResponse](s"$k8sApiServerURL/namespaces/default/pods/${container.name}")

            Future.successful(container)
          })
    }
  }

  trait ContainerCreator extends com.island.ohara.common.pattern.Creator[Future[Option[ContainerInfo]]] {
    def name(name: String): ContainerCreator

    def imageName(imageName: String): ContainerCreator

    def hostname(hostname: String): ContainerCreator

    def envs(envs: Map[String, String]): ContainerCreator

    def portMappings(ports: Map[Int, Int]): ContainerCreator

    def routes(routes: Map[String, String]): ContainerCreator

    def nodeName(nodeName: String): ContainerCreator

    /**
      * set the thread pool used to execute request
      * @param executionContext thread pool
      * @return this creator
      */
    @Optional("default pool is scala.concurrent.ExecutionContext.Implicits.global")
    def threadPool(executionContext: ExecutionContext): ContainerCreator

    override def create(): Future[Option[ContainerInfo]]

    def domainName(domainName: String): ContainerCreator

    def labelName(labelName: String): ContainerCreator

    def pullImagePolicy(imagePullPolicy: ImagePullPolicy): ContainerCreator

    def restartPolicy(restartPolicy: RestartPolicy): ContainerCreator

    def command(command: Seq[String]): ContainerCreator

    def args(args: Seq[String]): ContainerCreator
  }

  sealed abstract class ImagePullPolicy
  object ImagePullPolicy extends Enum[ImagePullPolicy] {

    case object ALWAYS extends ImagePullPolicy {
      override def toString: String = "Always"
    }
    case object NEVER extends ImagePullPolicy {
      override def toString: String = "Never"
    }
    case object IFNOTPRESENT extends ImagePullPolicy {
      override def toString: String = "IfNotPresent"
    }
  }

  sealed abstract class RestartPolicy
  object RestartPolicy extends Enum[RestartPolicy] {
    case object Always extends RestartPolicy {
      override def toString: String = "Always"
    }

    case object OnFailure extends RestartPolicy {
      override def toString: String = "OnFailure"
    }

    case object Never extends RestartPolicy {
      override def toString: String = "Never"
    }
  }

}
