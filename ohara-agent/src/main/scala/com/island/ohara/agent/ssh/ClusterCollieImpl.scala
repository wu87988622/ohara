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

package com.island.ohara.agent.ssh

import java.util.concurrent.{ExecutorService, TimeUnit}

import com.island.ohara.agent._
import com.island.ohara.agent.docker.DockerClient
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.{BrokerApi, ClusterStatus, StreamApi, WorkerApi, ZookeeperApi}
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.{CommonUtils, Releasable, ReleaseOnce}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

// accessible to configurator
private[ohara] class ClusterCollieImpl(cacheTimeout: Duration, nodeCollie: NodeCollie, cacheThreadPool: ExecutorService)
    extends ReleaseOnce
    with ClusterCollie {

  private[this] val dockerCache = DockerClientCache()

  private[this] val clusterCache: ClusterCache = ClusterCache.builder
    .frequency(cacheTimeout)
    // TODO: 5 * timeout is enough ??? by chia
    .supplier(() => Await.result(doClusters(ExecutionContext.fromExecutor(cacheThreadPool)), cacheTimeout * 5))
    // Giving some time to process to complete the build and then we can remove it from cache safety.
    .lazyRemove(cacheTimeout)
    .build()

  override val zookeeperCollie: ZookeeperCollie = new ZookeeperCollieImpl(nodeCollie, dockerCache, clusterCache)
  override val brokerCollie: BrokerCollie = new BrokerCollieImpl(nodeCollie, dockerCache, clusterCache)
  override val workerCollie: WorkerCollie = new WorkerCollieImpl(nodeCollie, dockerCache, clusterCache)
  override val streamCollie: StreamCollie = new StreamCollieImpl(nodeCollie, dockerCache, clusterCache)

  private[this] def doClusters(
    implicit executionContext: ExecutionContext): Future[Map[ClusterStatus, Seq[ContainerInfo]]] = nodeCollie
    .nodes()
    .flatMap(Future
      .traverse(_) { node =>
        // multi-thread to seek all containers from multi-nodes
        // Note: we fetch all containers (include exited and running) here
        dockerCache.exec(node, _.containers(containerName => containerName.startsWith(PREFIX_KEY))).recover {
          case e: Throwable =>
            LOG.error(s"failed to get active containers from $node", e)
            Seq.empty
        }
      }
      .map(_.flatten))
    .flatMap { allContainers =>
      def parse(
        serviceName: String,
        f: (ObjectKey, Seq[ContainerInfo]) => Future[ClusterStatus]): Future[Map[ClusterStatus, Seq[ContainerInfo]]] =
        Future
          .sequence(
            allContainers
              .filter(_.name.contains(s"$DIVIDER$serviceName$DIVIDER"))
              .map(container => Collie.objectKeyOfContainerName(container.name) -> container)
              .groupBy(_._1)
              .map {
                case (clusterKey, value) => clusterKey -> value.map(_._2)
              }
              .map {
                case (clusterKey, containers) => f(clusterKey, containers).map(_ -> containers)
              })
          .map(_.toMap)

      for {
        zkMap <- parse(ZookeeperApi.ZOOKEEPER_SERVICE_NAME, zookeeperCollie.toStatus)
        bkMap <- parse(BrokerApi.BROKER_SERVICE_NAME, brokerCollie.toStatus)
        wkMap <- parse(WorkerApi.WORKER_SERVICE_NAME, workerCollie.toStatus)
        streamMap <- parse(StreamApi.STREAM_SERVICE_NAME, streamCollie.toStatus)
      } yield zkMap ++ bkMap ++ wkMap ++ streamMap
    }

  override protected def doClose(): Unit = {
    Releasable.close(dockerCache)
    Releasable.close(clusterCache)
  }

  override def images(nodes: Seq[Node])(implicit executionContext: ExecutionContext): Future[Map[Node, Seq[String]]] =
    Future.traverse(nodes)(node => Future(dockerCache.exec(node, node -> _.imageNames()))).map(_.toMap)

  /**
    * The default implementation has the following checks.
    * 1) run hello-world image
    * 2) check existence of hello-world
    */
  override def verifyNode(node: Node)(implicit executionContext: ExecutionContext): Future[Try[String]] =
    Future.successful {
      Try {
        val name = CommonUtils.randomString(10)
        val dockerClient =
          DockerClient.builder.hostname(node.hostname).port(node._port).user(node._user).password(node._password).build
        try {
          val helloWorldImage = "hello-world"
          dockerClient.containerCreator().name(name).imageName(helloWorldImage).create()

          // TODO: should we directly reject the node which doesn't have hello-world image??? by chia
          def checkImage(): Boolean = {
            val endTime = CommonUtils.current() + 3 * 1000 // 3 seconds to timeout
            while (endTime >= CommonUtils.current()) {
              if (dockerClient.imageNames().contains(s"$helloWorldImage:latest")) return true
              else TimeUnit.SECONDS.sleep(1)
            }
            dockerClient.imageNames().contains(helloWorldImage)
          }

          // there are two checks.
          // 1) is there hello-world image?
          // 2) did we succeed to run hello-world container?
          if (!checkImage()) throw new IllegalStateException(s"Failed to download $helloWorldImage image")
          else if (dockerClient.containerNames().contains(name)) s"succeed to run $helloWorldImage on ${node.name}"
          else throw new IllegalStateException(s"failed to run container $helloWorldImage")
        } finally try dockerClient.forceRemove(name)
        finally dockerClient.close()
      }
    }
}
