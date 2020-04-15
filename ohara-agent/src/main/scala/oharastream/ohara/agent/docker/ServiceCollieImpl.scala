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

package oharastream.ohara.agent.docker

import java.util.concurrent.ExecutorService

import oharastream.ohara.agent._
import oharastream.ohara.agent.container.ContainerName
import oharastream.ohara.client.configurator.v0.ClusterStatus
import oharastream.ohara.client.configurator.v0.ClusterStatus.Kind
import oharastream.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import oharastream.ohara.client.configurator.v0.NodeApi.{Node, Resource}
import oharastream.ohara.common.setting.ObjectKey
import oharastream.ohara.common.util.Releasable

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

// accessible to configurator
private[ohara] class ServiceCollieImpl(cacheTimeout: Duration, dataCollie: DataCollie, cacheThreadPool: ExecutorService)
    extends ServiceCollie {
  private[this] val dockerClient = DockerClient(dataCollie)

  private[this] val clusterCache: ServiceCache = ServiceCache.builder
    .frequency(cacheTimeout)
    // TODO: 5 * timeout is enough ??? by chia
    .supplier(() => Await.result(doClusters(ExecutionContext.fromExecutor(cacheThreadPool)), cacheTimeout * 5))
    // Giving some time to process to complete the build and then we can remove it from cache safety.
    .lazyRemove(cacheTimeout)
    .build()

  override val zookeeperCollie: ZookeeperCollie = new BasicCollieImpl(dataCollie, dockerClient, clusterCache)
    with ZookeeperCollie
  override val brokerCollie: BrokerCollie = new BasicCollieImpl(dataCollie, dockerClient, clusterCache)
    with BrokerCollie
  override val workerCollie: WorkerCollie = new BasicCollieImpl(dataCollie, dockerClient, clusterCache)
    with WorkerCollie
  override val streamCollie: StreamCollie = new BasicCollieImpl(dataCollie, dockerClient, clusterCache)
    with StreamCollie
  override val shabondiCollie: ShabondiCollie = new BasicCollieImpl(dataCollie, dockerClient, clusterCache)
    with ShabondiCollie

  private[this] def doClusters(
    implicit executionContext: ExecutionContext
  ): Future[Seq[ClusterStatus]] =
    dockerClient
      .containers()
      .flatMap { allContainers =>
        def parse(
          kind: Kind,
          f: (ObjectKey, Seq[ContainerInfo]) => Future[ClusterStatus]
        ): Future[Seq[ClusterStatus]] =
          Future
            .sequence(
              allContainers
                .filter(container => Collie.matched(container.name, kind))
                .map(container => Collie.objectKeyOfContainerName(container.name) -> container)
                .groupBy(_._1)
                .map {
                  case (clusterKey, value) => clusterKey -> value.map(_._2)
                }
                .map {
                  case (clusterKey, containers) => f(clusterKey, containers)
                }
            )
            .map(_.toSeq)

        for {
          zkMap     <- parse(ClusterStatus.Kind.ZOOKEEPER, zookeeperCollie.toStatus)
          bkMap     <- parse(ClusterStatus.Kind.BROKER, brokerCollie.toStatus)
          wkMap     <- parse(ClusterStatus.Kind.WORKER, workerCollie.toStatus)
          streamMap <- parse(ClusterStatus.Kind.STREAM, streamCollie.toStatus)
        } yield zkMap ++ bkMap ++ wkMap ++ streamMap
      }

  override def close(): Unit = {
    Releasable.close(dockerClient)
    Releasable.close(clusterCache)
    Releasable.close(() => cacheThreadPool.shutdownNow())
  }

  override def imageNames()(implicit executionContext: ExecutionContext): Future[Map[Node, Seq[String]]] =
    dataCollie.values[Node]().flatMap { nodes =>
      Future
        .traverse(nodes) { node =>
          dockerClient.imageNames(node.name).map(images => node -> images)
        }
        .map(_.toMap)
    }

  /**
    * The default implementation has the following checks.
    * 1) run hello-world image
    * 2) check existence of hello-world
    */
  override def verifyNode(node: Node)(implicit executionContext: ExecutionContext): Future[String] =
    dockerClient
      .resources()
      .map { resources =>
        if (resources.getOrElse(node.hostname, Seq.empty).nonEmpty)
          s"succeed to check the docker resources on ${node.name}"
        else throw new IllegalStateException(s"the docker on ${node.hostname} is unavailable")
      }

  override def containerNames()(implicit executionContext: ExecutionContext): Future[Seq[ContainerName]] =
    dockerClient.containerNames()

  override def log(containerName: String, sinceSeconds: Option[Long])(
    implicit executionContext: ExecutionContext
  ): Future[Map[ContainerName, String]] = dockerClient.logs(containerName, sinceSeconds)

  override def resources()(implicit executionContext: ExecutionContext): Future[Map[String, Seq[Resource]]] =
    dockerClient.resources()
}
