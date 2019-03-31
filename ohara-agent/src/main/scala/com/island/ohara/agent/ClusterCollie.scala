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

package com.island.ohara.agent
import java.util.Objects
import java.util.concurrent.{ExecutorService, Executors, TimeUnit}

import com.island.ohara.agent.docker.DockerClient
import com.island.ohara.agent.k8s.{K8SClient, K8SClusterCollieImpl}
import com.island.ohara.agent.ssh.ClusterCollieImpl
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.{Node, NodeService}
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.client.configurator.v0.{ClusterInfo, NodeApi}
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.{CommonUtils, Releasable}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * This is the top-of-the-range "collie". It maintains and organizes all collies.
  * Each getter should return new instance of collie since each collie has close() method.
  * However, it is ok to keep global instance of collie if they have dump close().
  * Currently, default implementation is based on ssh and docker command. It is simple but slow.
  * TODO: We are looking for k8s implementation...by chia
  */
trait ClusterCollie extends Releasable {

  /**
    * create a collie for zookeeper cluster
    * @return zookeeper collie
    */
  def zookeeperCollie(): ZookeeperCollie

  /**
    * create a collie for broker cluster
    * @return broker collie
    */
  def brokerCollie(): BrokerCollie

  /**
    * create a collie for worker cluster
    * @return worker collie
    */
  def workerCollie(): WorkerCollie

  /**
    * the default implementation is expensive!!! Please override this method if you are a good programmer.
    * @return a collection of zk, bk and wk clusters
    */
  def clusters(implicit executionContext: ExecutionContext): Future[Map[ClusterInfo, Seq[ContainerInfo]]] =
    zookeeperCollie().clusters.flatMap { zkMap =>
      brokerCollie().clusters.flatMap { bkMap =>
        workerCollie().clusters.map { wkMap =>
          wkMap.map {
            case (wk, wkContainers) => (wk.asInstanceOf[ClusterInfo], wkContainers)
          } ++ bkMap.map {
            case (bk, bkContainers) => (bk.asInstanceOf[ClusterInfo], bkContainers)
          } ++ zkMap.map {
            case (zk, zkContainers) => (zk.asInstanceOf[ClusterInfo], zkContainers)
          }
        }
      }
    }

  /**
    * list the docker images hosted by input nodes
    * @param nodes remote nodes
    * @return the images stored by each node
    */
  def images(nodes: Seq[Node])(implicit executionContext: ExecutionContext): Future[Map[Node, Seq[String]]]

  /**
    * fetch all clusters and then update the services of input nodes.
    * NOTED: The input nodes which are not hosted by this cluster collie are not updated!!!
    * @param nodes nodes
    * @return updated nodes
    */
  def fetchServices(nodes: Seq[Node])(implicit executionContext: ExecutionContext): Future[Seq[Node]] =
    clusters.map(_.keys.toSeq).map { clusters =>
      nodes.map { node =>
        update(
          node = node,
          services = Seq(
            NodeService(
              name = NodeApi.ZOOKEEPER_SERVICE_NAME,
              clusterNames = clusters
                .filter(_.isInstanceOf[ZookeeperClusterInfo])
                .map(_.asInstanceOf[ZookeeperClusterInfo])
                .filter(_.nodeNames.contains(node.name))
                .map(_.name)
            ),
            NodeService(
              name = NodeApi.BROKER_SERVICE_NAME,
              clusterNames = clusters
                .filter(_.isInstanceOf[BrokerClusterInfo])
                .map(_.asInstanceOf[BrokerClusterInfo])
                .filter(_.nodeNames.contains(node.name))
                .map(_.name)
            ),
            NodeService(
              name = NodeApi.WORKER_SERVICE_NAME,
              clusterNames = clusters
                .filter(_.isInstanceOf[WorkerClusterInfo])
                .map(_.asInstanceOf[WorkerClusterInfo])
                .filter(_.nodeNames.contains(node.name))
                .map(_.name)
            )
          )
        )
      }
    }

  /**
    * In fake mode we use FakeNode instead of NodeImpl. Hence, we open a door to let fake CC override this method to
    * keep the fake implementation
    * @param node previous node
    * @param services new servies
    * @return update node
    */
  protected def update(node: Node, services: Seq[NodeService]): Node = NodeApi.copy(node, services)

  /**
    * Verify the node are available to be used in collie.
    * The default implementation has the following checks.
    * 1) run hello-world image
    * 2) check existence of hello-world
    * @param node validated node
    * @param executionContext thread pool
    * @return succeed report in string. Or try with exception
    */
  def verifyNode(node: Node)(implicit executionContext: ExecutionContext): Future[Try[String]] = Future {
    Try {
      val name = CommonUtils.randomString(10)
      val dockerClient =
        DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build()
      try {
        val helloWorldImage = "hello-world"
        dockerClient.containerCreator().name(name).imageName(helloWorldImage).execute()

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

object ClusterCollie {

  /**
    * the default implementation uses ssh and docker command to manage all clusters.
    * Each node running the service has name "${clusterName}-${service}-${index}".
    * For example, there is a worker cluster called "workercluster" and it is run on 3 nodes.
    * node-0 => workercluster-worker-0
    * node-1 => workercluster-worker-1
    * node-2 => workercluster-worker-2
    */
  def builderOfSsh(): SshBuilder = new SshBuilder

  import scala.concurrent.duration._

  class SshBuilder private[agent] {
    private[this] var nodeCollie: NodeCollie = _
    private[this] var expiredTime: Duration = 7 seconds
    private[this] var executor: ExecutorService = _

    def nodeCollie(nodeCollie: NodeCollie): SshBuilder = {
      this.nodeCollie = Objects.requireNonNull(nodeCollie)
      this
    }

    @Optional("default is 7 seconds")
    def expiredTime(expiredTime: Duration): SshBuilder = {
      this.expiredTime = Objects.requireNonNull(expiredTime)
      this
    }

    /**
      * set a thread pool that initial size is equal with number of cores
      * @return this builder
      */
    def executorDefault(): SshBuilder = executor(Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors()))

    def executor(executor: ExecutorService): SshBuilder = {
      this.executor = Objects.requireNonNull(executor)
      this
    }

    /**
      * We don't return ClusterCollieImpl since it is a private implementation
      * @return
      */
    def build(): ClusterCollie = new ClusterCollieImpl(
      expiredTime = Objects.requireNonNull(expiredTime),
      nodeCollie = Objects.requireNonNull(nodeCollie),
      executor = Objects.requireNonNull(executor)
    )

  }

  /**
    * create kubernetes implements
    * @param nodeCollie
    * @return
    */
  def k8s(implicit nodeCollie: NodeCollie, k8sClient: K8SClient): ClusterCollie = new K8SClusterCollieImpl
}
