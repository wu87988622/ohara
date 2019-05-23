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

import com.island.ohara.agent._
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

private class K8SBrokerCollieImpl(val nodeCollie: NodeCollie, val k8sClient: K8SClient)
    extends K8SBasicCollieImpl[BrokerClusterInfo, BrokerCollie.ClusterCreator](nodeCollie, k8sClient)
    with BrokerCollie {

  private[this] val LOG = Logger(classOf[K8SBrokerCollieImpl])
  private[this] val TIMEOUT: FiniteDuration = 30 seconds

  override def creator(): BrokerCollie.ClusterCreator =
    (executionContext, clusterName, imageName, zookeeperClusterName, clientPort, exporterPort, jmxPort, nodeNames) => {
      implicit val exec: ExecutionContext = executionContext
      exist(clusterName)
        .flatMap(if (_) containers(clusterName) else Future.successful(Seq.empty))
        .flatMap(
          existContainers =>
            nodeCollie
              .nodes(existContainers.map(_.nodeName))
              .map(_.zipWithIndex.map {
                case (node, index) => node -> existContainers(index)
              }.toMap)
              .map { existNodes =>
                // if there is a running cluster already, we should check the consistency of configuration
                existNodes.values.foreach {
                  container =>
                    def checkValue(previous: String, newValue: String): Unit =
                      if (previous != newValue) throw new IllegalArgumentException(s"previous:$previous new:$newValue")
                    def check(key: String, newValue: String): Unit = {
                      val previous = container.environments(key)
                      if (previous != newValue) throw new IllegalArgumentException(s"previous:$previous new:$newValue")
                    }
                    checkValue(container.imageName, imageName)
                    check(BrokerCollie.CLIENT_PORT_KEY, clientPort.toString)
                    check(BrokerCollie.ZOOKEEPER_CLUSTER_NAME, zookeeperClusterName)
                }
                existNodes
            })
        .flatMap(existNodes =>
          nodeCollie
            .nodes(nodeNames)
            .map(_.map(node => node -> s"${format(PREFIX_KEY, clusterName, serviceName)}$DIVIDER${node.name}").toMap)
            .map((existNodes, _)))
        .flatMap {
          case (existNodes, newNodes) =>
            existNodes.keys.foreach(node =>
              if (newNodes.keys.exists(_.name == node.name))
                throw new IllegalArgumentException(s"${node.name} has run the broker service for $clusterName"))

            query(zookeeperClusterName, ContainerCollie.ZK_SERVICE_NAME).map((existNodes, newNodes, _))
        }
        .map {
          case (existNodes, newNodes, zkContainers) =>
            if (zkContainers.isEmpty) throw new IllegalArgumentException(s"$clusterName doesn't exist")
            val zookeepers = zkContainers
              .map(c => s"${c.hostname}.$K8S_DOMAIN_NAME:${c.environments(ZookeeperCollie.CLIENT_PORT_KEY).toInt}")
              .mkString(",")

            val maxId: Int =
              if (existNodes.isEmpty) 0
              else existNodes.values.map(_.environments(BrokerCollie.ID_KEY).toInt).toSet.max + 1

            val successfulNodeNames = newNodes.zipWithIndex
              .map {
                case ((node, hostname), index) =>
                  val client = k8sClient
                  try {
                    val creator: Future[Option[ContainerInfo]] = client
                      .containerCreator()
                      .imageName(imageName)
                      .nodename(node.name)
                      .labelName(OHARA_LABEL)
                      .domainName(K8S_DOMAIN_NAME)
                      .portMappings(Map(
                        clientPort -> clientPort,
                        exporterPort -> exporterPort,
                        jmxPort -> jmxPort
                      ))
                      .hostname(hostname)
                      .envs(Map(
                        BrokerCollie.ID_KEY -> (maxId + index).toString,
                        BrokerCollie.CLIENT_PORT_KEY -> clientPort.toString,
                        BrokerCollie.ZOOKEEPERS_KEY -> zookeepers,
                        BrokerCollie.ADVERTISED_HOSTNAME_KEY -> node.name,
                        BrokerCollie.EXPORTER_PORT_KEY -> exporterPort.toString,
                        BrokerCollie.ADVERTISED_CLIENT_PORT_KEY -> clientPort.toString,
                        BrokerCollie.ZOOKEEPER_CLUSTER_NAME -> zookeeperClusterName,
                        BrokerCollie.JMX_HOSTNAME_KEY -> node.name,
                        BrokerCollie.JMX_PORT_KEY -> jmxPort.toString
                      ))
                      .name(hostname)
                      .run()
                    Await.result(creator, TIMEOUT)
                  } catch {
                    case e: Throwable =>
                      LOG.error(s"failed to start $imageName on ${node.name}", e)
                      None
                  }
              }
              .map(_.get.nodeName)
              .toSeq
            if (successfulNodeNames.isEmpty)
              throw new IllegalArgumentException(s"failed to create $clusterName on Broker")
            BrokerClusterInfo(
              name = clusterName,
              imageName = imageName,
              zookeeperClusterName = zookeeperClusterName,
              exporterPort = exporterPort,
              clientPort = clientPort,
              jmxPort = jmxPort,
              nodeNames = successfulNodeNames ++ existNodes.map(_._1.name)
            )
        }
    }

  override protected def toClusterDescription(clusterName: String, containers: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[BrokerClusterInfo] = toBrokerCluster(clusterName, containers)
}
