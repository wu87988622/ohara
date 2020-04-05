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

package oharastream.ohara.it

import java.net.URL
import java.util.Objects

import oharastream.ohara.agent.DataCollie
import oharastream.ohara.agent.container.ContainerClient
import oharastream.ohara.agent.docker.DockerClient
import oharastream.ohara.agent.k8s.K8SClient
import oharastream.ohara.client.configurator.v0.NodeApi.{Node, State}
import oharastream.ohara.common.util.{CommonUtils, VersionUtils}
import org.junit.AssumptionViolatedException

import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

trait ContainerPlatform {
  def configuratorHostname: String
  def nodes: Seq[Node]
  def containerClient: ContainerClient
  def arguments: Seq[String]
}

object ContainerPlatform {
  private[this] def result[T](f: Future[T]): T = Await.result(f, 60 seconds)

  val K8S_MASTER_KEY: String    = "ohara.it.k8s"
  val K8S_METRICS_SERVER_URL    = "ohara.it.k8s.metrics.server"
  val K8S_NAMESPACE_KEY: String = "ohara.it.k8s.namespace"

  /**
    * form: user:password@hostname:port.
    * NOTED: this key need to be matched with another key value in ohara-it/build.gradle
    */
  val DOCKER_NODES_KEY = "ohara.it.docker"
  private[this] def _ks8Mode: Option[ContainerPlatform] =
    sys.env
      .get(ContainerPlatform.K8S_MASTER_KEY)
      .map { masterUrl =>
        val metricsUrl = sys.env.get(ContainerPlatform.K8S_METRICS_SERVER_URL).orNull
        val containerClient = K8SClient.builder
          .apiServerURL(masterUrl)
          .namespace(sys.env.getOrElse(K8S_NAMESPACE_KEY, "default"))
          .metricsApiServerURL(metricsUrl)
          .build()
        ContainerPlatform.builder
        // the master node is NOT able to run pods by default so we must exclude it
          .excludedHostname(new URL(masterUrl).getHost)
          .modeName("K8S")
          .nodes(result(containerClient.nodes()).map(_.nodeName).map(Node.apply))
          .containerClient(containerClient)
          .arguments(
            Seq(
              "--k8s",
              masterUrl
            ) ++ (if (metricsUrl == null) Seq.empty
                  else
                    Seq(
                      "--k8s-metrics-server",
                      metricsUrl
                    ))
          )
          .build
      }

  /**
    * @return k8s platform information. Or skip test
    */
  def k8sMode: ContainerPlatform =
    _dockerMode.getOrElse(
      throw new AssumptionViolatedException(s"set ${ContainerPlatform.K8S_MASTER_KEY} to run IT on k8s mode")
    )

  private[this] def _dockerMode: Option[ContainerPlatform] = {
    def parserNode(nodeInfo: String): Node = {
      val user     = nodeInfo.split(":").head
      val password = nodeInfo.split("@").head.split(":").last
      val hostname = nodeInfo.split("@").last.split(":").head
      val port     = nodeInfo.split("@").last.split(":").last.toInt
      Node(
        hostname = hostname,
        port = Some(port),
        user = Some(user),
        password = Some(password),
        services = Seq.empty,
        state = State.AVAILABLE,
        error = None,
        lastModified = CommonUtils.current(),
        resources = Seq.empty,
        tags = Map.empty
      )
    }

    sys.env
      .get(ContainerPlatform.DOCKER_NODES_KEY)
      .map(plainString => plainString.split(",").map(parserNode).toSeq)
      .map { nodes =>
        println(s"[CHIA] nodes:$nodes")
        ContainerPlatform.builder
          .modeName("DOCKER")
          .nodes(nodes)
          .containerClient(DockerClient(DataCollie(nodes)))
          .build
      }
  }

  /**
    * @return docker platform information. Or skip test
    */
  def dockerMode: ContainerPlatform =
    _dockerMode.getOrElse(
      throw new AssumptionViolatedException(s"set ${ContainerPlatform.DOCKER_NODES_KEY} to run IT on docker mode")
    )

  private[this] val ERROR_MESSAGE = s"please set ${ContainerPlatform.K8S_MASTER_KEY} and ${ContainerPlatform.K8S_METRICS_SERVER_URL}" +
    s"to run the IT on k8s mode; Or set ${ContainerPlatform.DOCKER_NODES_KEY} to run IT on docker mode"

  /**
    * The order of lookup is shown below.
    * 1) k8s setting - PlatformModeInfo.K8S_MASTER_KEY and PlatformModeInfo.K8S_METRICS_SERVER_URL
    * 2) docker setting - PlatformModeInfo.DOCKER_NODES_KEY
    * @return one of k8s or docker. If they are nonexistent, a AssumptionViolatedException is thrown
    */
  def default: ContainerPlatform =
    _ks8Mode.orElse(_dockerMode).getOrElse(throw new AssumptionViolatedException(ERROR_MESSAGE))

  /**
    * @return k8s + docker. Or empty collection
    */
  def all: Seq[ContainerPlatform] = (_dockerMode ++ _ks8Mode).toSeq

  /**
    * @return a empty platform that all methods throw AssumptionViolatedException
    */
  def empty: ContainerPlatform = new ContainerPlatform {
    private[this] val exception = new AssumptionViolatedException(ERROR_MESSAGE)

    override def nodes: Seq[Node] = throw exception

    override def containerClient: ContainerClient = throw exception

    override def arguments: Seq[String] = throw exception

    override def toString: String = "EMPTY"

    override def configuratorHostname: String = throw exception
  }

  def builder = new Builder

  private[ContainerPlatform] class Builder extends oharastream.ohara.common.pattern.Builder[ContainerPlatform] {
    private[this] var modeName: String                 = _
    private[this] var nodes: Seq[Node]                 = Seq.empty
    private[this] var containerClient: ContainerClient = _
    private[this] var arguments: Seq[String]           = Seq.empty
    private[this] var excludedHostname: Option[String] = None

    def modeName(modeName: String): Builder = {
      this.modeName = CommonUtils.requireNonEmpty(modeName)
      this
    }

    def nodes(nodes: Seq[Node]): Builder = {
      this.nodes = CommonUtils.requireNonEmpty(nodes.asJava).asScala
      this
    }

    def containerClient(containerClient: ContainerClient): Builder = {
      this.containerClient = Objects.requireNonNull(containerClient)
      this
    }

    def arguments(arguments: Seq[String]): Builder = {
      this.arguments = Objects.requireNonNull(arguments)
      this
    }

    /**
      * don't select this node as configurator node
      * @param hostname excluded hostname
      * @return this builder
      */
    def excludedHostname(hostname: String): Builder = {
      this.excludedHostname = Some(hostname)
      this
    }

    override def build: ContainerPlatform = {
      val hostname = {
        val images = result(containerClient.imageNames())
        images
          .filter(_._2.contains(s"oharastream/configurator:${VersionUtils.VERSION}"))
          .filterNot(e => excludedHostname.contains(e._1))
          .keys
          .headOption
          .getOrElse(
            throw new RuntimeException(
              s"failed to find oharastream/configurator:${VersionUtils.VERSION} from nodes:${images.keySet.mkString(",")}"
            )
          )
      }
      println(s"[CHIA] configuratorHostname:$hostname")
      new ContainerPlatform {
        override val nodes: Seq[Node] = CommonUtils.requireNonEmpty(Builder.this.nodes.asJava).asScala

        override val containerClient: ContainerClient = Objects.requireNonNull(Builder.this.containerClient)

        override val arguments: Seq[String] = Objects.requireNonNull(Builder.this.arguments)

        override val toString: String = CommonUtils.requireNonEmpty(modeName)

        override def configuratorHostname: String = hostname
      }
    }
  }
}
