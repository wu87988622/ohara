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

package com.island.ohara.agent.wharf.stream

import java.util.Objects

import com.island.ohara.agent.docker.DockerClient
import com.island.ohara.agent.wharf.Cargo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.StreamApi
import com.island.ohara.common.util.CommonUtils
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}

sealed abstract class StreamCargo extends Cargo {

  /**
    * single entry to create the stream container (use docker by default)
    *
    * @return the creator
    */
  def createContainer(): StreamCargo.ContainerCreator
}
private[stream] object StreamCargo {

  private[this] val LOG = Logger(StreamCargo.getClass)

  //TODO : DockerClient operate container... we need to control clean up after graceful shutdown
  def apply(client: DockerClient): StreamCargo = new StreamCargo {

    override def createContainer(): ContainerCreator = new ContainerCreator(name, client)

    override def info(): Future[ContainerInfo] = {
      Future.successful(client.container(name))
    }

    override def removeContainer(isForce: Boolean)(implicit executionContext: ExecutionContext): Future[Boolean] = {
      Future {
        if (isForce) client.forceRemove(name)
        else client.remove(name)
      }.map(_ => true).recover {
        case e: Throwable =>
          LOG.error(s"container remove exception : ${e.getMessage}")
          false
      }
    }
  }

  class ContainerCreator(name: String, client: DockerClient) extends Cargo.ContainerCreator {
    // set hostname as same as container name to simplify implementation
    private[this] val _hostname: String = name
    private[this] var _jarUrl: String = _
    private[this] var _appId: String = _
    private[this] var _brokerList: Seq[String] = _
    private[this] var _fromTopics: Seq[String] = _
    private[this] var _toTopics: Seq[String] = _

    def jarUrl(jarUrl: String): ContainerCreator = {
      this._jarUrl = CommonUtils.requireNonEmpty(jarUrl)
      this
    }
    def appId(appId: String): ContainerCreator = {
      this._appId = CommonUtils.requireNonEmpty(appId)
      this
    }
    def brokerList(brokerList: Seq[String]): ContainerCreator = {
      this._brokerList = brokerList
      this
    }
    def fromTopics(fromTopics: Seq[String]): ContainerCreator = {
      this._fromTopics = notEmpty(fromTopics)
      this
    }
    def toTopics(toTopics: Seq[String]): ContainerCreator = {
      this._toTopics = notEmpty(toTopics)
      this
    }

    override def create()(implicit executionContext: ExecutionContext): Future[ContainerInfo] = doCreate(
      client = client,
      executionContext = Objects.requireNonNull(executionContext),
      name = CommonUtils.requireNonEmpty(name),
      hostname = CommonUtils.requireNonEmpty(_hostname),
      imageName = CommonUtils.requireNonEmpty(imageName),
      jarUrl = CommonUtils.requireNonEmpty(_jarUrl),
      appId = CommonUtils.requireNonEmpty(_appId),
      brokerList = notEmpty(_brokerList),
      fromTopics = notEmpty(_fromTopics),
      toTopics = notEmpty(_toTopics)
    )

    private[this] def doCreate(
      client: DockerClient,
      executionContext: ExecutionContext,
      name: String,
      hostname: String,
      imageName: String,
      jarUrl: String,
      appId: String,
      brokerList: Seq[String],
      fromTopics: Seq[String],
      toTopics: Seq[String]
    ): Future[ContainerInfo] = {
      implicit val exec: ExecutionContext = executionContext
      Future
        .successful(
          client.containerCreator()
        )
        .map { creator =>
          creator
            .name(name)
            .hostname(hostname)
            .imageName(imageName)
            .envs(
              Map(
                StreamApi.JARURL_KEY -> jarUrl,
                StreamApi.APPID_KEY -> appId,
                StreamApi.SERVERS_KEY -> brokerList.mkString(","),
                StreamApi.FROM_TOPIC_KEY -> fromTopics.mkString(","),
                StreamApi.TO_TOPIC_KEY -> toTopics.mkString(",")
              )
            )
            .execute()
        }
        .map(_ => client.container(name))
    }
  }

  private[this] def notEmpty(list: Seq[String]): Seq[String] = {
    list.map(CommonUtils.requireNonEmpty)
  }
}
