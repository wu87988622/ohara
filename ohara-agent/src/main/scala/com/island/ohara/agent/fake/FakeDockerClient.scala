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

package com.island.ohara.agent.fake

import java.util.Date

import com.island.ohara.agent.docker.DockerClient.ContainerInspector
import com.island.ohara.agent.docker.{ContainerCreator, ContainerState, DockerClient, NetworkDriver}
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, PortMapping, PortPair}
import com.island.ohara.common.util.{CommonUtils, ReleaseOnce}
import com.typesafe.scalalogging.Logger

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

private[agent] class FakeDockerClient extends ReleaseOnce with DockerClient {
  private val LOG = Logger(classOf[FakeDockerClient])
  private[this] val FAKE_KIND_NAME: String = "FAKE"
  private[this] val cachedContainers = new mutable.HashMap[String, ContainerInfo]()

  override def containerNames(): Seq[String] = cachedContainers.keys.toSeq

  private[this] def listContainers(nameFilter: String => Boolean): Future[Seq[ContainerInfo]] =
    Future.successful(cachedContainers.filter { case (name, _) => nameFilter(name) }.values.toSeq)

  override def containers(nameFilter: String => Boolean)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] = listContainers(nameFilter)

  //there is no meaning of "active" in fake mode
  override def activeContainers(nameFilter: String => Boolean)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] = listContainers(nameFilter)

  override def containerCreator(): ContainerCreator = (hostname: String,
                                                       imageName: String,
                                                       name: String,
                                                       _: String,
                                                       _: Boolean,
                                                       ports: Map[Int, Int],
                                                       envs: Map[String, String],
                                                       _: Map[String, String],
                                                       _: Map[String, String],
                                                       _: NetworkDriver) =>
    cachedContainers.put(
      name,
      ContainerInfo(
        nodeName = hostname,
        id = name,
        imageName = imageName,
        created = new Date(CommonUtils.current()).toString,
        state = ContainerState.RUNNING.name,
        kind = FAKE_KIND_NAME,
        name = name,
        size = "-999 MB",
        portMappings =
          if (ports.isEmpty) Seq.empty
          else
            Seq(PortMapping(hostname, ports.map {
              case (port, containerPort) =>
                PortPair(port, containerPort)
            }.toSeq)),
        environments = envs,
        hostname = hostname
      )
  )

  override def stop(name: String): Unit =
    cachedContainers.update(name, cachedContainers(name).copy(state = ContainerState.EXITED.name))

  override def remove(name: String): Unit = cachedContainers.remove(name)

  override def forceRemove(name: String): Unit = cachedContainers.remove(name)

  override def verify(): Boolean = true

  override def log(name: String): String = s"fake docker log for $name"

  override def containerInspector(containerName: String): ContainerInspector = containerInspector(containerName, false)

  private[this] def containerInspector(containerName: String, beRoot: Boolean): ContainerInspector =
    new ContainerInspector {
      private[this] def rootConfig: String = if (beRoot) "-u root" else ""
      override def cat(path: String): Option[String] =
        Some(s"""docker exec $rootConfig $containerName /bin/bash -c \"cat $path\"""")

      override def append(path: String, content: Seq[String]): String = {
        LOG.info(
          s"""docker exec $rootConfig $containerName /bin/bash -c \"echo \\"${content.mkString("\n")}\\" >> $path\"""")
        cat(path).get
      }

      override def write(path: String, content: Seq[String]): String = {
        LOG.info(
          s"""docker exec $rootConfig $containerName /bin/bash -c \"echo \\"${content.mkString("\n")}\\" > $path\"""")
        cat(path).get
      }

      override def asRoot(): ContainerInspector = containerInspector(containerName, true)
    }

  override def imageNames(): Seq[String] = cachedContainers.values.map(_.imageName).toSeq

  override def toString: String = getClass.getName

  override protected def doClose(): Unit = LOG.info("close client")

  override def container(name: String): ContainerInfo = cachedContainers(name)
}
