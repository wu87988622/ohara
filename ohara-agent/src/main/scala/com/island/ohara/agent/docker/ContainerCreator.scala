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

import com.island.ohara.agent.{Agent, NetworkDriver}
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.CommonUtils
import com.typesafe.scalalogging.Logger

/**
  * A interface used to run a docker container on remote node
  */
trait ContainerCreator {

  /**
    *
    * @return this container name
    */
  def getContainerName: String

  /**
    * set container's name. default is a random string
    *
    * @param name container name
    * @return this builder
    */
  @Optional("default is CommonUtils.randomString()")
  def name(name: String): ContainerCreator

  /**
    * set target image
    *
    * @param imageName docker image
    * @return this builder
    */
  def imageName(imageName: String): ContainerCreator

  /**
    * @param hostname the hostname of container
    * @return this builder
    */
  def hostname(hostname: String): ContainerCreator

  /**
    * @param envs the env variables exposed to container
    * @return this builder
    */
  def envs(envs: Map[String, String]): ContainerCreator

  /**
    * @param route the pre-defined route to container. hostname -> ip
    * @return this builder
    */
  def route(route: Map[String, String]): ContainerCreator

  /**
    * forward the port from host to container.
    * NOTED: currently we don't support to specify the network interface so the forwarded port is bound on all network adapters.
    *
    * @param ports port mapping (host's port -> container's port)
    * @return this builder
    */
  def portMappings(ports: Map[Int, Int]): ContainerCreator

  /**
    * docker -v
    *
    * @return process information
    */
  def volumeMapping(volumeMapping: Map[String, String]): ContainerCreator

  /**
    * set docker container's network driver. implement by --network=$value
    *
    * @param driver network driver
    * @return this builder
    */
  @Optional("default is NetworkDriver.BRIDGE")
  def networkDriver(driver: NetworkDriver): ContainerCreator

  /**
    * set true if you want to clean up the dead container automatically
    *
    * @return executor
    */
  def cleanup(): ContainerCreator

  /**
    * the command passed to docker container
    *
    * @param command command
    * @return this builder
    */
  def command(command: String): ContainerCreator

  /**
    * execute the docker container on background.
    */
  def execute(): Unit

  /**
    * this is used in testing. Developers can check the generated command by this method.
    *
    * @return the command used to start docker container
    */
  protected[agent] def dockerCommand(): String
}

private[agent] object ContainerCreator {

  def apply(agent: Agent): ContainerCreator = new ContainerCreator {
    private val LOG = Logger(classOf[ContainerCreator])

    private[this] var hostname: String = _
    private[this] var imageName: String = _
    private[this] var name: String = CommonUtils.randomString()
    private[this] var command: String = _
    private[this] var disableCleanup: Boolean = true
    private[this] var ports: Map[Int, Int] = Map.empty
    private[this] var envs: Map[String, String] = Map.empty
    private[this] var route: Map[String, String] = Map.empty
    private[this] var volumeMapping: Map[String, String] = Map.empty
    private[this] var networkDriver: NetworkDriver = NetworkDriver.BRIDGE

    override def getContainerName: String = this.name

    override def name(name: String): ContainerCreator = {
      this.name = CommonUtils.requireNonEmpty(name)
      this
    }

    override def imageName(imageName: String): ContainerCreator = {
      this.imageName = CommonUtils.requireNonEmpty(imageName)
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

    override def route(route: Map[String, String]): ContainerCreator = {
      this.route = route
      this
    }

    override def portMappings(ports: Map[Int, Int]): ContainerCreator = {
      this.ports = ports
      this
    }

    override def volumeMapping(volumeMapping: Map[String, String]): ContainerCreator = {
      this.volumeMapping = volumeMapping
      this
    }

    override def networkDriver(driver: NetworkDriver): ContainerCreator = {
      this.networkDriver = driver
      this
    }

    override def cleanup(): ContainerCreator = {
      this.disableCleanup = false
      this
    }

    override def command(command: String): ContainerCreator = {
      this.command = command
      this
    }

    override def execute(): Unit = {
      val cmd = dockerCommand()
      LOG.info(s"docker command:$cmd")
      agent.execute(cmd)
    }

    override protected[agent] def dockerCommand(): String = {
      Seq(
        "docker run -d ",
        if (hostname == null) "" else s"-h $hostname",
        route
          .map {
            case (host, ip) => s"--add-host $host:$ip"
          }
          .mkString(" "),
        if (disableCleanup) "" else "--rm",
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
  }
}
