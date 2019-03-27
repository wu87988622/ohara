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

import com.island.ohara.agent.docker.DockerClient.ContainerInspector
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, ContainerState}
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.Releasable

import scala.concurrent.{ExecutionContext, Future}

/**
  * An interface used to control remote node's docker service.
  * the default implementation is based on ssh client.
  * NOTED: All containers are executed background so as to avoid blocking call.
  */
trait DockerClient extends Releasable {

  /**
    * @param name container's name
    * @return true if container exists. otherwise, false
    */
  def exist(name: String): Boolean = containerNames().contains(name)

  /**
    * @param name container's name
    * @return true if container does not exist. otherwise, true
    */
  def nonExist(name: String): Boolean = !exist(name)

  /**
    * @return a collection of running docker containers
    */
  def activeContainers(implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    containers.map(_.filter(_.state == ContainerState.RUNNING.name))

  /**
    * the filter is used to reduce the possible communication across ssh.
    * @return a collection of running docker containers
    */
  def activeContainers(nameFilter: String => Boolean)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]]

  /**
    * @return all containers' name
    */
  def containerNames(): Seq[String]

  /**
    * @return a collection of docker containers
    */
  def containers(implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] = containers(_ => true)

  /**
    * the filter is used to reduce the possible communication across ssh.
    * @return a collection of docker containers
    */
  def containers(nameFilter: String => Boolean)(implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]]

  /**
    * @param name container's name
    * @return container description or None if container doesn't exist
    */
  def container(name: String): ContainerInfo

  /**
    * build a docker container.
    * @return a container builder
    */
  def containerCreator(): ContainerCreator

  /**
    * stop a running container. If the container doesn't exist, exception will be thrown.
    * @param name container's name
    */
  def stop(name: String): Unit

  /**
    * remove a stopped container. If the container doesn't exist, exception will be thrown.
    * @param name container's name
    */
  def remove(name: String): Unit

  /**
    * remove a container. If the container doesn't exist, exception will be thrown.
    * @param name container's name
    */
  def forceRemove(name: String): Unit

  /**
    * check whether the remote node is capable of running docker.
    * @return true if remote node succeed to run hello-world container. otherwise, false.
    */
  def verify(): Boolean

  /**
    * get the console log from the container
    * @param name container's name
    * @return log
    */
  def log(name: String): String

  def imageNames(): Seq[String]

  def containerInspector(containerName: String): ContainerInspector
}

object DockerClient {
  def builder(): Builder = new Builder

  /**
    * used to "touch" a running container. For example, you can cat a file from a running container
    */
  trait ContainerInspector {

    /**
      * convert the user to root. If the files accessed by inspect requires the root permission, you can run this method
      * before doing inspect action.
      * @return a new ContainerInspector with root permission
      */
    def asRoot(): ContainerInspector

    /**
      * get content of specified file from a container.
      * This method is useful in debugging when you want to check something according to the file content.
      * @param path file path
      * @return content of file
      */
    def cat(path: String): Option[String]

    /**
      * append something to the file of a running container
      * @param content content
      * @param path file path
      */
    def append(path: String, content: String): String = append(path, Seq(content))

    /**
      * append something to the file of a running container
      * @param content content
      * @param path file path
      */
    def append(path: String, content: Seq[String]): String

    /**
      * clear and write something to the file of a running container
      * @param content content
      * @param path file path
      */
    def write(path: String, content: String): String = write(path, Seq(content))

    /**
      * clear and write something to the file of a running container
      * @param content content
      * @param path file path
      */
    def write(path: String, content: Seq[String]): String
  }

  class Builder private[agent] {
    private[this] var hostname: String = _
    private[this] var port: Int = 22
    private[this] var user: String = _
    private[this] var password: String = _

    def hostname(hostname: String): Builder = {
      this.hostname = hostname
      this
    }

    @Optional("default port is 22")
    def port(port: Int): Builder = {
      this.port = port
      this
    }

    def user(user: String): Builder = {
      this.user = user
      this
    }

    def password(password: String): Builder = {
      this.password = password
      this
    }

    def build(): DockerClient = new DockerClientImpl(Objects.requireNonNull(hostname),
                                                     port,
                                                     Objects.requireNonNull(user),
                                                     Objects.requireNonNull(password))
  }
}
