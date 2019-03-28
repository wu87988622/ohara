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
import java.util.concurrent.ConcurrentHashMap

import com.island.ohara.agent.ssh.DockerClientCache
import com.island.ohara.agent.wharf.{Cargo, Warehouse}
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.util.CommonUtils

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

sealed class StreamWarehouse(nodes: Seq[Node],
                             warehouseName: String,
                             imageName: String,
                             jarUrl: String,
                             appId: String,
                             brokerList: Seq[String],
                             fromTopics: Seq[String],
                             toTopics: Seq[String],
                             clientCache: DockerClientCache)
    extends Warehouse {
  private[this] val cargoes = new ConcurrentHashMap[Cargo, Cargo]()

  override def name: String = warehouseName

  override def list()(implicit executionContext: ExecutionContext): Future[Seq[Cargo]] =
    Future.successful(cargoes.keys().asScala.toSeq)

  override def add(nodeName: String)(implicit executionContext: ExecutionContext): Future[Cargo] = {
    Future {
      clientCache.exec(
        nodes.find(_.name == nodeName).getOrElse(throw new Exception(s"cannot find node in crane : $nodeName")),
        StreamCargo(_))
    }.flatMap { cargo =>
        cargo
          .createContainer()
          .imageName(imageName)
          .jarUrl(jarUrl)
          .appId(appId)
          .brokerList(brokerList)
          .fromTopics(fromTopics)
          .toTopics(toTopics)
          .create()
          .map(_ => cargo)
      }
      .map { cargo =>
        cargoes.put(cargo, cargo)
        cargo
      }
  }

  def add(nodeNames: Seq[String])(implicit executionContext: ExecutionContext): Future[Seq[Cargo]] =
    Future.traverse(nodeNames)(add)

  override def delete(cargoName: String)(implicit executionContext: ExecutionContext): Future[Cargo] =
    list()
      .map { cgs =>
        cgs.find(_.name == cargoName).getOrElse(throw new Exception(s"not exists cargo : $cargoName"))
      }
      .flatMap { cg =>
        cg.removeContainer(false).map {
          case true =>
            cargoes.remove(cg)
          case false => throw new RuntimeException(s"cannot delete cargo, what's wrong?")
        }
      }

  def deleteAll()(implicit executionContext: ExecutionContext): Future[Seq[Cargo]] =
    Future.traverse(cargoes.keys().asScala.map(_.name).toSeq)(delete)
}

private[stream] object StreamWarehouse {
  // give the unique name to create the warehouse
  def apply(nodes: Seq[Node],
            warehouseName: String,
            imageName: String,
            jarUrl: String,
            appId: String,
            brokerList: Seq[String],
            fromTopics: Seq[String],
            toTopics: Seq[String],
            isFake: Boolean): StreamWarehouse = new StreamWarehouse(
    Objects.requireNonNull(nodes),
    CommonUtils.requireNonEmpty(warehouseName),
    CommonUtils.requireNonEmpty(imageName),
    CommonUtils.requireNonEmpty(jarUrl),
    CommonUtils.requireNonEmpty(appId),
    Objects.requireNonNull(brokerList),
    Objects.requireNonNull(fromTopics),
    Objects.requireNonNull(toTopics),
    // We use docker client to manage container here
    // TODO : Maybe change to support other implementation?...by Sam
    if (isFake) DockerClientCache.fake() else DockerClientCache()
  )
}
