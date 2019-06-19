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

package com.island.ohara.agent.docker

import java.util.Objects

import com.island.ohara.agent.Agent
import com.island.ohara.agent.docker.DockerClient.ContainerInspector
import com.island.ohara.agent.docker.DockerClientImpl._
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, PortMapping, PortPair}
import com.island.ohara.common.annotations.VisibleForTesting
import com.island.ohara.common.util.{Releasable, ReleaseOnce}
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}

private[docker] object DockerClientImpl {
  private val LOG = Logger(classOf[DockerClientImpl])

  /**
    * Parse the forward ports from container.
    * NOTED: the form of docker's port is shown below:
    * 1) 0.0.0.0:12345-12350->12345-12350/tcp
    * 2) 0.0.0.0:2181->2181/tcp, 0.0.0.0:2888->2888/tcp, 0.0.0.0:3888->3888/tcp
    * This method is exposed to testing scope
    * @param portMapping port mapping string
    * @return (host's ip -> (host's port -> container's port)
    */
  def parsePortMapping(portMapping: String): Seq[PortMapping] = {
    portMapping
      .replaceAll("/tcp", "")
      // we don't distinguish protocol now.. TODO by chia
      .replaceAll("/udp", "")
      .split(", ")
      .flatMap { portMap =>
        val portPair = portMap.split("->")
        if (portPair.length != 2) throw new IllegalArgumentException(s"invalid format: $portPair")

        def parsePort(portsString: String): Seq[Int] = if (portsString.contains("-"))
          portsString.split("-").head.toInt to portsString.split("-").last.toInt
        else Seq(portsString.toInt)
        if (portPair.head.split(":").length != 2)
          throw new IllegalArgumentException(s"Illegal ${portPair.head} (expected hostname:port)")
        val network: String = portPair.head.split(":").head
        val hostPorts: Seq[Int] = parsePort(portPair.head.split(":").last)
        val containerPort: Seq[Int] = parsePort(portPair.last)
        if (hostPorts.size != containerPort.size)
          throw new IllegalArgumentException("the size of host's port is not matched to size of container's port")
        hostPorts.zipWithIndex.map {
          case (port, index) => (network, port, containerPort(index))
        }
      }
      .groupBy(_._1)
      .map {
        case (key, value) => PortMapping(key, value.map(v => PortPair(v._2, v._3)).toSeq)
      }
      .toSeq
  }

  val DIVIDER: String = ",,"

  val LIST_PROCESS_FORMAT: String = Seq(
    "{{.ID}}",
    "{{.Image}}",
    "{{.CreatedAt}}",
    "{{.Status}}",
    "{{.Names}}",
    "{{.Size}}",
    "{{.Ports}}"
  ).mkString(DIVIDER)

  @VisibleForTesting
  private[docker] def toSshCommand(hostname: String,
                                   imageName: String,
                                   name: String,
                                   command: String,
                                   removeContainerOnExit: Boolean,
                                   ports: Map[Int, Int],
                                   envs: Map[String, String],
                                   route: Map[String, String],
                                   volumeMapping: Map[String, String],
                                   networkDriver: NetworkDriver): String = Seq(
    "docker run -d ",
    if (hostname == null) "" else s"-h $hostname",
    route
      .map {
        case (host, ip) => s"--add-host $host:$ip"
      }
      .mkString(" "),
    if (removeContainerOnExit) "--rm" else "",
    s"--name ${Objects.requireNonNull(name)}",
    ports
      .map {
        case (hostPort, containerPort) => s"-p $hostPort:$containerPort"
      }
      .mkString(" "),
    envs
      .map {
        case (key, value) => s"""-e \"$key=$value\""""
      }
      .mkString(" "),
    volumeMapping
      .map {
        case (key, value) => s"""-v \"$key:$value\""""
      }
      .mkString(" "),
    networkDriver match {
      case NetworkDriver.HOST   => "--network=host"
      case NetworkDriver.BRIDGE => "--network=bridge"
    },
    Objects.requireNonNull(imageName),
    if (command == null) "" else command
  ).filter(_.nonEmpty).mkString(" ")
}
private[docker] class DockerClientImpl(nodeName: String, port: Int, user: String, password: String)
    extends ReleaseOnce
    with DockerClient {
  private[this] val agent = Agent.builder
    .hostname(Objects.requireNonNull(nodeName))
    .port(port)
    .user(Objects.requireNonNull(user))
    .password(Objects.requireNonNull(password))
    .build

  override protected def doClose(): Unit = Releasable.close(agent)

  override def containerCreator(): ContainerCreator = (hostname: String,
                                                       imageName: String,
                                                       name: String,
                                                       command: String,
                                                       removeContainerOnExit: Boolean,
                                                       ports: Map[Int, Int],
                                                       envs: Map[String, String],
                                                       route: Map[String, String],
                                                       volumeMapping: Map[String, String],
                                                       networkDriver: NetworkDriver) =>
    agent.execute(
      DockerClientImpl.toSshCommand(
        hostname = hostname,
        imageName = imageName,
        name = name,
        command = command,
        removeContainerOnExit = removeContainerOnExit,
        ports = ports,
        envs = envs,
        route = route,
        volumeMapping = volumeMapping,
        networkDriver = networkDriver
      ))

  override def containerNames(): Seq[String] =
    agent.execute("docker ps -a --format {{.Names}}").map(_.split("\n").toSeq).getOrElse(Seq.empty)

  override def activeContainers(nameFilter: String => Boolean)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] = listContainers(nameFilter, true)

  override def containers(nameFilter: String => Boolean)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] = listContainers(nameFilter, false)

  private[this] def listContainers(nameFilter: String => Boolean, active: Boolean)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    Future
      .traverse(
        try agent
          .execute(
            if (active) s"docker ps --format $LIST_PROCESS_FORMAT" else s"docker ps -a --format $LIST_PROCESS_FORMAT")
          .map(_.split("\n").toSeq.filter(line => nameFilter(line.split(DIVIDER).filter(_.nonEmpty)(4))))
          .getOrElse(Seq.empty)
        catch {
          case e: Throwable =>
            LOG.error(s"failed to list containers on $agent", e)
            Seq.empty
        }) { line =>
        Future { Some(toContainerInfo(line)) }.recover {
          case e: Throwable =>
            LOG.error(
              s"failed to get container description from $nodeName." +
                "This error may be caused by operator conflict since we can't get container information by single command.",
              e
            )
            None
        }
      }
      .map(_.flatten)

  private[this] def toContainerInfo(line: String): ContainerInfo = {
    val SSH_KIND_NAME = "SSH"
    // filter out all empty string
    val items = line.split(DIVIDER).filter(_.nonEmpty).toSeq
    // not all containers have forward ports so length - 1
    if (items.length != LIST_PROCESS_FORMAT.split(DIVIDER).length
        && items.length != LIST_PROCESS_FORMAT.split(DIVIDER).length - 1)
      throw new IllegalArgumentException(
        s"the expected number of items in $line is ${LIST_PROCESS_FORMAT.split(DIVIDER).length} or ${LIST_PROCESS_FORMAT.split(DIVIDER).length - 1}")
    val id = items.head
    ContainerInfo(
      nodeName = nodeName,
      id = id,
      imageName = items(1),
      created = items(2),
      state = ContainerState.all
        .find(s => items(3).toLowerCase.contains(s.name.toLowerCase))
        // the running container show "up to xxx"
        .getOrElse(ContainerState.RUNNING)
        .name,
      kind = SSH_KIND_NAME,
      name = items(4),
      size = items(5),
      portMappings = if (items.size < 7) Seq.empty else parsePortMapping(items(6)),
      environments = agent
        .execute(s"docker inspect $id --format '{{.Config.Env}}'")
        .map(_.replaceAll("\n", ""))
        // form: [abc=123 aa=111]
        .filter(_.length > 2)
        .map(_.substring(1))
        .map(s => s.substring(0, s.length - 1))
        .map { s =>
          s.split(" ")
            .filterNot(_.isEmpty)
            .map { line =>
              val items = line.split("=")
              items.size match {
                case 1 => items.head -> ""
                case 2 => items.head -> items.last
                case _ => throw new IllegalArgumentException(s"invalid format of environment:$line")
              }
            }
            .toMap
        }
        .getOrElse(Map.empty),
      // we can't cat file from a exited container
      hostname = agent
        .execute(s"docker inspect $id --format '{{.Config.Hostname}}'")
        // remove new line
        .map(_.replaceAll("\n", ""))
        .get
    )
  }

  override def stop(name: String): Unit = agent.execute(s"docker stop $name")

  override def remove(name: String): Unit = agent.execute(s"docker rm $name")

  override def forceRemove(name: String): Unit = agent.execute(s"docker rm -f $name")

  override def verify(): Boolean =
    agent.execute("docker run --rm hello-world").exists(_.contains("Hello from Docker!"))

  override def log(name: String): String = agent
    .execute(s"docker container logs $name")
    .map(msg => if (msg.contains("ERROR:")) throw new IllegalArgumentException(msg) else msg)
    .getOrElse(throw new IllegalArgumentException(s"no response from $nodeName"))

  override def containerInspector(containerName: String): ContainerInspector = containerInspector(containerName, false)

  private[this] def containerInspector(containerName: String, beRoot: Boolean): ContainerInspector =
    new ContainerInspector {
      private[this] def rootConfig: String = if (beRoot) "-u root" else ""
      override def cat(path: String): Option[String] =
        agent.execute(s"""docker exec $rootConfig $containerName /bin/bash -c \"cat $path\"""")

      override def append(path: String, content: Seq[String]): String = {
        agent.execute(
          s"""docker exec $rootConfig $containerName /bin/bash -c \"echo \\"${content.mkString("\n")}\\" >> $path\"""")
        cat(path).get
      }

      override def write(path: String, content: Seq[String]): String = {
        agent.execute(
          s"""docker exec $rootConfig $containerName /bin/bash -c \"echo \\"${content.mkString("\n")}\\" > $path\"""")
        cat(path).get
      }

      override def asRoot(): ContainerInspector = containerInspector(containerName, true)
    }

  override def imageNames(): Seq[String] = agent
    .execute("docker images --format {{.Repository}}:{{.Tag}}")
    .map(_.split("\n").toSeq)
    .filter(_.nonEmpty)
    .getOrElse(Seq.empty)

  override def toString: String = s"$user@$nodeName:$port"

  override def container(name: String): ContainerInfo = toContainerInfo(
    agent
      .execute(s"docker ps -a --format $LIST_PROCESS_FORMAT")
      .map(_.split("\n").toSeq.filter(line => line.split(DIVIDER).filter(_.nonEmpty)(4) == name))
      .getOrElse(throw new NoSuchElementException(s"$name doesn't exist"))
      .head)
}
