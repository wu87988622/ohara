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

import java.util.Objects

import oharastream.ohara.agent.container.ContainerClient
import oharastream.ohara.client.configurator.v0.NodeApi.Node
import oharastream.ohara.common.util.CommonUtils
import org.junit.AssumptionViolatedException

import scala.collection.JavaConverters._
trait PlatformModeInfo {
  def nodes: Seq[Node]
  def containerClient: ContainerClient
  def arguments: Seq[String]
}

object PlatformModeInfo {
  def empty: PlatformModeInfo = new PlatformModeInfo {
    private[this] val exception = new AssumptionViolatedException(
      s"please set ${EnvTestingUtils.K8S_MASTER_KEY}, ${EnvTestingUtils.K8S_METRICS_SERVER_URL} and ${EnvTestingUtils.K8S_NODES_KEY}" +
        s"to run the IT on k8s mode; Or set ${EnvTestingUtils.DOCKER_NODES_KEY} to run IT on docker mode"
    )

    override def nodes: Seq[Node] = throw exception

    override def containerClient: ContainerClient = throw exception

    override def arguments: Seq[String] = throw exception

    override def toString: String = "EMPTY"
  }

  def builder = new Builder

  class Builder private[PlatformModeInfo] extends oharastream.ohara.common.pattern.Builder[PlatformModeInfo] {
    private[this] var modeName: String                 = _
    private[this] var nodes: Seq[Node]                 = Seq.empty
    private[this] var containerClient: ContainerClient = _
    private[this] var arguments: Seq[String]           = Seq.empty

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

    override def build: PlatformModeInfo = new PlatformModeInfo {
      override val nodes: Seq[Node] = CommonUtils.requireNonEmpty(Builder.this.nodes.asJava).asScala

      override val containerClient: ContainerClient = Objects.requireNonNull(Builder.this.containerClient)

      override val arguments: Seq[String] = Objects.requireNonNull(Builder.this.arguments)

      override val toString: String = CommonUtils.requireNonEmpty(modeName)
    }
  }
}
