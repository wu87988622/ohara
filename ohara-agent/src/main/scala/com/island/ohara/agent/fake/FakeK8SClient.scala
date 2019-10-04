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

import java.util.concurrent.ConcurrentHashMap

import com.island.ohara.agent.k8s.{K8SClient, K8SJson, K8SStatusInfo, Report}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.{BrokerApi, ContainerApi, WorkerApi, ZookeeperApi}

import scala.concurrent.{ExecutionContext, Future}

class FakeK8SClient(isK8SNode: Boolean, k8sStatusInfo: Option[K8SStatusInfo], containerName: String) extends K8SClient {
  private[this] val cachedConfigs = new ConcurrentHashMap[String, Map[String, String]]()

  override def images(nodeName: String)(implicit executionContext: ExecutionContext): Future[Seq[String]] =
    Future.successful {
      Seq(ZookeeperApi.IMAGE_NAME_DEFAULT, BrokerApi.IMAGE_NAME_DEFAULT, WorkerApi.IMAGE_NAME_DEFAULT)
    }

  override def checkNode(nodeName: String)(implicit executionContext: ExecutionContext): Future[Report] =
    Future.successful(
      Report(nodeName, isK8SNode, k8sStatusInfo)
    )

  override def containers()(implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    Future.successful {
      Seq(
        ContainerInfo("node1",
                      "0000",
                      "fakeimage",
                      "2019-05-21 00:00:00",
                      "running",
                      "unknow",
                      containerName,
                      "0",
                      Seq.empty,
                      Map.empty,
                      "host1"))
    }

  override def remove(name: String)(implicit executionContext: ExecutionContext): Future[ContainerInfo] =
    throw new UnsupportedOperationException("FakeK8SClient not support remove function")

  override def removeNode(clusterName: String, nodeName: String, serviceName: String)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerApi.ContainerInfo]] =
    throw new UnsupportedOperationException("FakeK8SClient not support remove node function")

  override def log(name: String)(implicit executionContext: ExecutionContext): Future[String] =
    Future.successful(s"fake k8s log for $name")

  override def nodeNameIPInfo()(implicit executionContext: ExecutionContext): Future[Seq[K8SJson.HostAliases]] =
    Future.successful(Seq.empty)

  override def addConfig(name: String, configs: Map[String, String])(
    implicit executionContext: ExecutionContext): Future[String] =
    Future.successful {
      cachedConfigs.put(name, configs)
      name
    }

  override def removeConfig(name: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.successful(cachedConfigs.remove(name)).map(_ => true)
  override def forceRemoveConfig(name: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.successful(cachedConfigs.remove(name)).map(_ => true)

  override def inspectConfig(name: String)(implicit executionContext: ExecutionContext): Future[Map[String, String]] =
    Future.successful(cachedConfigs.get(name))

  override def containerCreator()(implicit executionContext: ExecutionContext): K8SClient.ContainerCreator =
    throw new UnsupportedOperationException("FakeK8SClient not support containerCreator function")

  override def forceRemove(name: String)(implicit executionContext: ExecutionContext): Future[ContainerInfo] =
    throw new UnsupportedOperationException("FakeK8SClient not support force remove function")
}
