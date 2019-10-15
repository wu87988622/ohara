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

import com.island.ohara.agent.{BrokerCollie, NoSuchClusterException, NodeCollie, StreamCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.FileInfoApi.FileInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterStatus
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.streams.config.StreamDefUtils
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}

private class K8SStreamCollieImpl(node: NodeCollie, bkCollie: BrokerCollie, k8sClient: K8SClient)
    extends K8SBasicCollieImpl[StreamClusterStatus](node, k8sClient)
    with StreamCollie {
  private[this] val LOG = Logger(classOf[K8SStreamCollieImpl])

  override protected def doCreator(executionContext: ExecutionContext,
                                   containerName: String,
                                   containerInfo: ContainerInfo,
                                   node: Node,
                                   route: Map[String, String],
                                   jmxPort: Int,
                                   jarInfo: FileInfo): Future[Unit] = {
    implicit val exec: ExecutionContext = executionContext
    k8sClient
      .containerCreator()
      .imageName(containerInfo.imageName)
      .nodeName(containerInfo.nodeName)
      /**
        * the hostname of k8s/docker container has strict limit. Fortunately, we are aware of this issue and the hostname
        * passed to this method is legal to k8s/docker. Hence, assigning the hostname is very safe to you :)
        */
      .hostname(containerInfo.hostname)
      .name(containerInfo.name)
      .labelName(OHARA_LABEL)
      .domainName(K8S_DOMAIN_NAME)
      .portMappings(Map(jmxPort -> jmxPort))
      .routes(route)
      .envs(containerInfo.environments)
      .args(StreamCollie.formatJMXProperties(node.name, jmxPort) ++
        Seq(StreamCollie.MAIN_ENTRY,
            s"""${StreamDefUtils.JAR_URL_DEFINITION.key()}=${jarInfo.url.toURI.toASCIIString}"""))
      .threadPool(executionContext)
      .create()
      .recover {
        case e: Throwable =>
          LOG.error(s"failed to start ${containerInfo.imageName} on ${node.name}", e)
          None
      }
      .map(_ => Unit)
  }

  override protected def doRemoveNode(previousCluster: StreamClusterStatus, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.failed(
      new UnsupportedOperationException("stream collie doesn't support to remove node from a running cluster"))

  override protected def nodeCollie: NodeCollie = node
  override protected def prefixKey: String = PREFIX_KEY

  override protected def brokerContainers(clusterKey: ObjectKey)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    bkCollie
      .clusters()
      .map(
        _.find(_._1.key == clusterKey)
          .map(_._2)
          .getOrElse(throw new NoSuchClusterException(s"broker cluster:$clusterKey does not exist")))
}
