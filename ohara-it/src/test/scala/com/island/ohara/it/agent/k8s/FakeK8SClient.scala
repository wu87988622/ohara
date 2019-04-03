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

package com.island.ohara.it.agent.k8s

import com.island.ohara.agent.k8s.{K8SClient, K8SJson}
import com.island.ohara.client.configurator.v0.ContainerApi

import scala.concurrent.{ExecutionContext, Future}

class FakeK8SClient(k8sNode: Boolean, nodeHealth: Boolean) extends K8SClient {
  override def containers(implicit executionContext: ExecutionContext): Seq[ContainerApi.ContainerInfo] = ???

  override def remove(name: String)(implicit executionContext: ExecutionContext): ContainerApi.ContainerInfo = ???

  override def removeNode(clusterName: String, nodeName: String, serviceName: String)(
    implicit executionContext: ExecutionContext): Seq[ContainerApi.ContainerInfo] = ???

  override def log(name: String): String = ???

  override def nodeNameIPInfo(implicit executionContext: ExecutionContext): Seq[K8SJson.HostAliases] = ???

  override def containerCreator(): K8SClient.ContainerCreator = ???

  override def images(nodeName: String)(implicit executionContext: ExecutionContext): Future[Seq[String]] = ???

  override def isK8SNode(nodeName: String)(implicit executionContext: ExecutionContext): Future[Boolean] = Future {
    k8sNode
  }

  override def isNodeHealth(nodeName: String)(implicit executionContext: ExecutionContext): Future[Boolean] = Future {
    nodeHealth
  }

  /** Do what you want to do when calling closing. */
  override protected def doClose(): Unit = ???
}
