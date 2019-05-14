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
import com.island.ohara.client.configurator.v0.BrokerApi
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext, Future}

private class K8SWorkerCollieImpl(val nodeCollie: NodeCollie, val k8sClient: K8SClient)
    extends K8SBasicCollieImpl[WorkerClusterInfo, WorkerCollie.ClusterCreator](nodeCollie, k8sClient)
    with WorkerCollie {
  private[this] val LOG = Logger(classOf[K8SWorkerCollieImpl])
  private[this] val TIMEOUT: FiniteDuration = 30 seconds
  private[this] val BROKER_CLUSTER_NAME = ClusterCollie.BROKER_CLUSTER_NAME

  override def creator(): WorkerCollie.ClusterCreator = (executionContext,
                                                         clusterName,
                                                         imageName,
                                                         brokerClusterName,
                                                         clientPort,
                                                         jmxPort,
                                                         groupId,
                                                         offsetTopicName,
                                                         offsetTopicReplications,
                                                         offsetTopicPartitions,
                                                         statusTopicName,
                                                         statusTopicReplications,
                                                         statusTopicPartitions,
                                                         configTopicName,
                                                         configTopicReplications,
                                                         jarUrls,
                                                         nodeNames) => {
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
                  check(WorkerCollie.GROUP_ID_KEY, groupId)
                  check(WorkerCollie.OFFSET_TOPIC_KEY, offsetTopicName)
                  check(WorkerCollie.OFFSET_TOPIC_PARTITIONS_KEY, offsetTopicPartitions.toString)
                  check(WorkerCollie.OFFSET_TOPIC_REPLICATIONS_KEY, offsetTopicReplications.toString)
                  check(WorkerCollie.STATUS_TOPIC_KEY, statusTopicName)
                  check(WorkerCollie.STATUS_TOPIC_PARTITIONS_KEY, statusTopicPartitions.toString)
                  check(WorkerCollie.STATUS_TOPIC_REPLICATIONS_KEY, statusTopicReplications.toString)
                  check(WorkerCollie.CONFIG_TOPIC_KEY, configTopicName)
                  check(WorkerCollie.CONFIG_TOPIC_REPLICATIONS_KEY, configTopicReplications.toString)
                  check(WorkerCollie.CLIENT_PORT_KEY, clientPort.toString)
                  check(BROKER_CLUSTER_NAME, brokerClusterName)
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
              throw new IllegalArgumentException(s"${node.name} has run the worker service for $clusterName"))
          query(brokerClusterName, ContainerCollie.BK_SERVICE_NAME).map((existNodes, newNodes, _))
      }
      .map {
        case (existNodes, newNodes, brokerContainers) =>
          if (brokerContainers.isEmpty)
            throw new IllegalArgumentException(s"broker cluster:$brokerClusterName doesn't exist")
          val brokers = brokerContainers
            .map(c =>
              s"${c.hostname}.$K8S_DOMAIN_NAME:${c.environments.getOrElse(BrokerCollie.CLIENT_PORT_KEY, BrokerApi.CLIENT_PORT_DEFAULT)}")
            .mkString(",")

          val successfulNodeNames = newNodes
            .map {
              case (node, hostname) =>
                val client = k8sClient
                try {
                  val creator: Future[Option[ContainerInfo]] = client
                    .containerCreator()
                    .imageName(imageName)
                    .portMappings(Map(clientPort -> clientPort, jmxPort -> jmxPort))
                    .hostname(hostname)
                    .nodename(node.name)
                    .envs(Map(
                      WorkerCollie.CLIENT_PORT_KEY -> clientPort.toString,
                      WorkerCollie.BROKERS_KEY -> brokers,
                      WorkerCollie.GROUP_ID_KEY -> groupId,
                      WorkerCollie.OFFSET_TOPIC_KEY -> offsetTopicName,
                      WorkerCollie.OFFSET_TOPIC_PARTITIONS_KEY -> offsetTopicPartitions.toString,
                      WorkerCollie.OFFSET_TOPIC_REPLICATIONS_KEY -> offsetTopicReplications.toString,
                      WorkerCollie.CONFIG_TOPIC_KEY -> configTopicName,
                      WorkerCollie.CONFIG_TOPIC_REPLICATIONS_KEY -> configTopicReplications.toString,
                      WorkerCollie.STATUS_TOPIC_KEY -> statusTopicName,
                      WorkerCollie.STATUS_TOPIC_PARTITIONS_KEY -> statusTopicPartitions.toString,
                      WorkerCollie.STATUS_TOPIC_REPLICATIONS_KEY -> statusTopicReplications.toString,
                      WorkerCollie.ADVERTISED_HOSTNAME_KEY -> node.name,
                      WorkerCollie.ADVERTISED_CLIENT_PORT_KEY -> clientPort.toString,
                      WorkerCollie.JARS_KEY -> jarUrls.mkString(","),
                      BROKER_CLUSTER_NAME -> brokerClusterName,
                      WorkerCollie.JMX_HOSTNAME_KEY -> node.name,
                      WorkerCollie.JMX_PORT_KEY -> jmxPort.toString
                    ))
                    .labelName(OHARA_LABEL)
                    .domainName(K8S_DOMAIN_NAME)
                    .name(hostname)
                    .run()
                  Await.result(creator, TIMEOUT)
                } catch {
                  case e: Throwable =>
                    LOG.error(s"failed to start $imageName", e)
                    None
                }
            }
            .map(_.get.nodeName)
            .toSeq
          if (successfulNodeNames.isEmpty)
            throw new IllegalArgumentException(s"failed to create $clusterName on Worker")
          WorkerClusterInfo(
            name = clusterName,
            imageName = imageName,
            brokerClusterName = brokerClusterName,
            clientPort = clientPort,
            jmxPort = jmxPort,
            groupId = groupId,
            offsetTopicName = offsetTopicName,
            offsetTopicPartitions = offsetTopicPartitions,
            offsetTopicReplications = offsetTopicReplications,
            configTopicName = configTopicName,
            configTopicPartitions = 1,
            configTopicReplications = configTopicReplications,
            statusTopicName = statusTopicName,
            statusTopicPartitions = statusTopicPartitions,
            statusTopicReplications = statusTopicReplications,
            jarIds = jarUrls.map(_.getFile),
            jarUrls = jarUrls,
            connectors = Seq.empty,
            nodeNames = successfulNodeNames ++ existNodes.map(_._1.name)
          )
      }
  }

  override protected def toClusterDescription(clusterName: String, containers: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[WorkerClusterInfo] = toWorkerCluster(clusterName, containers)
}
