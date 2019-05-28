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

package com.island.ohara.configurator

import java.io.File
import java.util.Objects

import com.island.ohara.agent._
import com.island.ohara.agent.k8s.K8SClient
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.NodeApi
import com.island.ohara.client.configurator.v0.NodeApi.{Node, NodeService}
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.annotations.{Optional, VisibleForTesting}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.fake._
import com.island.ohara.configurator.jar.JarStore
import com.island.ohara.configurator.store.DataStore

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
class ConfiguratorBuilder {
  private[this] var hostname: String = CommonUtils.hostname()
  private[this] var port: Int = CommonUtils.availablePort()
  private[this] var homeFolder: String = CommonUtils.createTempFolder("configurator").getCanonicalPath
  private[this] var store: DataStore = _
  private[this] var clusterCollie: ClusterCollie = _
  private[this] var k8sClient: K8SClient = _

  @Optional("default is random folder")
  def homeFolder(homeFolder: String): ConfiguratorBuilder = {
    if (store != null)
      throw new IllegalArgumentException("you have instantiated a store so you can't change the home folder")
    val f = new File(CommonUtils.requireNonEmpty(homeFolder))
    if (!f.exists() && !f.mkdirs()) throw new IllegalArgumentException(s"failed to mkdir on $homeFolder")
    this.homeFolder = CommonUtils.requireFolder(f).getCanonicalPath
    this
  }

  /**
    * set advertised hostname which will be exposed by configurator.
    *
    * @param hostname used to build the rest server
    * @return this builder
    */
  @Optional("default is localhost")
  def hostname(hostname: String): ConfiguratorBuilder = {
    this.hostname = CommonUtils.requireNonEmpty(hostname)
    this
  }

  /**
    * set advertised port which will be exposed by configurator.
    * Noted: configurator is bound on this port also.
    * @param port used to build the rest server
    * @return this builder
    */
  @Optional("default is random port")
  def port(port: Int): ConfiguratorBuilder = {
    if (port > 0) this.port = port
    this
  }

  /**
    * set all client to fake mode with a pre-created broker cluster and worker cluster.
    *
    * @return this builder
    */
  @VisibleForTesting
  private[configurator] def fake(): ConfiguratorBuilder = fake(1, 1)

  /**
    * set all client to fake mode but broker client and worker client is true that they are connecting to embedded cluster.
    *
    * @return this builder
    */
  @VisibleForTesting
  private[configurator] def fake(bkConnectionProps: String, wkConnectionProps: String): ConfiguratorBuilder = {
    if (k8sClient != null)
      throw new IllegalArgumentException("k8s client exists so you can't run Configurator in fake mode")
    val store = getOrCreateStore()
    val embeddedBkName = "embedded_broker_cluster"
    val embeddedWkName = "embedded_worker_cluster"
    // we fake nodes for embedded bk and wk
    def nodes(s: String): Seq[String] = s.split(",").map(_.split(":").head)
    import scala.concurrent.ExecutionContext.Implicits.global
    (nodes(bkConnectionProps) ++ nodes(wkConnectionProps))
    // DON'T add duplicate nodes!!!
      .toSet[String]
      .map { nodeName =>
        Node(
          name = nodeName,
          services = (if (bkConnectionProps.contains(nodeName))
                        Seq(NodeService(NodeApi.BROKER_SERVICE_NAME, Seq(embeddedBkName)))
                      else Seq.empty) ++ (if (wkConnectionProps.contains(nodeName))
                                            Seq(NodeService(NodeApi.WORKER_SERVICE_NAME, Seq(embeddedWkName)))
                                          else Seq.empty),
          port = -1,
          user = "fake user",
          password = "fake password",
          lastModified = CommonUtils.current()
        )
      }
      .foreach(store.add)
    val collie = new FakeClusterCollie(createCollie(), store, bkConnectionProps, wkConnectionProps)
    val bkCluster = {
      val pair = bkConnectionProps.split(",")
      val host = pair.map(_.split(":").head).head
      val port = pair.map(_.split(":").last).head.toInt
      BrokerClusterInfo(
        name = embeddedBkName,
        imageName = "None",
        zookeeperClusterName = "None",
        exporterPort = -1,
        jmxPort = -1,
        clientPort = port,
        nodeNames = Seq(host)
      )
    }
    val wkCluster = {
      val pair = wkConnectionProps.split(",")
      val host = pair.map(_.split(":").head).head
      val port = pair.map(_.split(":").last).head.toInt
      WorkerClusterInfo(
        name = embeddedWkName,
        imageName = "None",
        brokerClusterName = bkCluster.name,
        clientPort = port,
        // Assigning a negative value can make test fail quickly.
        jmxPort = -1,
        groupId = "None",
        statusTopicName = "None",
        statusTopicPartitions = 1,
        statusTopicReplications = 1.asInstanceOf[Short],
        configTopicName = "None",
        configTopicPartitions = 1,
        configTopicReplications = 1.asInstanceOf[Short],
        offsetTopicName = "None",
        offsetTopicPartitions = 1,
        offsetTopicReplications = 1.asInstanceOf[Short],
        jarInfos = Seq.empty,
        connectors = Await.result(WorkerClient(wkConnectionProps).connectors, 10 seconds),
        nodeNames = Seq(host)
      )
    }
    collie.brokerCollie().addCluster(bkCluster)
    collie.workerCollie().addCluster(wkCluster)
    clusterCollie(collie)
  }

  /**
    * Create a fake collie with specified number of broker/worker cluster.
    * @param numberOfBrokerCluster number of broker cluster
    * @param numberOfWorkerCluster number of worker cluster
    * @return this builder
    */
  @VisibleForTesting
  private[configurator] def fake(numberOfBrokerCluster: Int,
                                 numberOfWorkerCluster: Int,
                                 zkClusterNamePrefix: String = "fakezkcluster",
                                 bkClusterNamePrefix: String = "fakebkcluster",
                                 wkClusterNamePrefix: String = "fakewkcluster"): ConfiguratorBuilder = {
    if (k8sClient != null)
      throw new IllegalArgumentException("k8s client exists so you can't run Configurator in fake mode")
    if (numberOfBrokerCluster < 0)
      throw new IllegalArgumentException(s"numberOfBrokerCluster:$numberOfBrokerCluster should be positive")
    if (numberOfWorkerCluster < 0)
      throw new IllegalArgumentException(s"numberOfWorkerCluster:$numberOfWorkerCluster should be positive")
    if (numberOfBrokerCluster <= 0 && numberOfWorkerCluster > 0)
      throw new IllegalArgumentException(s"you must initialize bk cluster before you initialize wk cluster")
    val store = getOrCreateStore()
    val collie = new FakeClusterCollie(createCollie(), store)

    val zkClusters = (0 until numberOfBrokerCluster).map { index =>
      collie
        .zookeeperCollie()
        .addCluster(FakeZookeeperClusterInfo(
          name = s"$zkClusterNamePrefix$index",
          imageName = s"fakeImage$index",
          // Assigning a negative value can make test fail quickly.
          clientPort = -1,
          electionPort = -1,
          peerPort = -1,
          nodeNames = (0 to 2).map(_ => CommonUtils.randomString(5))
        ))
    }

    // add broker cluster
    val bkClusters = zkClusters.zipWithIndex.map {
      case (zkCluster, index) =>
        collie
          .brokerCollie()
          .addCluster(FakeBrokerClusterInfo(
            name = s"$bkClusterNamePrefix$index",
            imageName = s"fakeImage$index",
            zookeeperClusterName = zkCluster.name,
            // Assigning a negative value can make test fail quickly.
            clientPort = -1,
            exporterPort = -1,
            jmxPort = -1,
            nodeNames = zkCluster.nodeNames
          ))
    }

    // we don't need to collect wk clusters
    (0 until numberOfWorkerCluster).foreach { index =>
      val bkCluster = bkClusters((Math.random() % bkClusters.size).asInstanceOf[Int])
      collie
        .workerCollie()
        .addCluster(FakeWorkerClusterInfo(
          name = s"$wkClusterNamePrefix$index",
          imageName = s"fakeImage$index",
          brokerClusterName = bkCluster.name,
          // Assigning a negative value can make test fail quickly.
          clientPort = -1,
          // Assigning a negative value can make test fail quickly.
          jmxPort = -1,
          groupId = s"groupId$index",
          statusTopicName = s"statusTopicName$index",
          statusTopicPartitions = 1,
          statusTopicReplications = 1.asInstanceOf[Short],
          configTopicName = s"configTopicName$index",
          configTopicPartitions = 1,
          configTopicReplications = 1.asInstanceOf[Short],
          offsetTopicName = s"offsetTopicName$index",
          offsetTopicPartitions = 1,
          offsetTopicReplications = 1.asInstanceOf[Short],
          jarInfos = Seq.empty,
          connectors = Seq.empty,
          sources = Seq.empty,
          sinks = Seq.empty,
          nodeNames = bkCluster.nodeNames
        ))
    }

    import scala.concurrent.ExecutionContext.Implicits.global
    // fake nodes
    zkClusters
      .flatMap(_.nodeNames)
      // DON'T add duplicate nodes!!!
      .toSet[String]
      .map(
        name =>
          Node(name = name,
               port = -1,
               user = "fake user",
               password = "fake password",
               services = Seq.empty,
               lastModified = CommonUtils.current()))
      .foreach(store.add[Node])
    clusterCollie(collie)
  }

  @VisibleForTesting
  @Optional("default is implemented by ssh")
  private[configurator] def clusterCollie(clusterCollie: ClusterCollie): ConfiguratorBuilder = {
    if (this.clusterCollie != null) throw new IllegalArgumentException(s"cluster collie is defined!!!")
    this.clusterCollie = Objects.requireNonNull(clusterCollie)
    this
  }

  /**
    * Set a k8s client to enable container collie to use k8s platform. If you don't set it, the default implementation apply the ssh connection
    * to control containers on remote nodes.
    * @param k8sClient k8s client
    * @return this builder
    */
  @Optional("default is null")
  def k8sClient(k8sClient: K8SClient): ConfiguratorBuilder = {
    if (this.k8sClient != null) throw new IllegalArgumentException(s"k8sClient is defined!!!")
    this.k8sClient = Objects.requireNonNull(k8sClient)
    this
  }

  private[configurator] def createCollie(): NodeCollie = {
    val store = getOrCreateStore()
    new NodeCollie {
      override def node(name: String)(implicit executionContext: ExecutionContext): Future[Node] =
        store.value[Node](name)
      override def nodes()(implicit executionContext: ExecutionContext): Future[Seq[Node]] = store.values[Node]()
    }
  }

  def build(): Configurator =
    new Configurator(hostname = hostname, port = port)(
      store = getOrCreateStore(),
      jarStore = JarStore.builder.homeFolder(folder("jars")).hostname(hostname).port(port).build(),
      nodeCollie = createCollie(),
      clusterCollie = getOrCreateCollie(),
      k8sClient = Option(k8sClient)
    )

  private[this] def folder(prefix: String): String =
    new File(CommonUtils.requireNonEmpty(homeFolder), prefix).getCanonicalPath

  private[this] def getOrCreateStore(): DataStore = if (store == null) {
    store = DataStore.builder.persistentFolder(folder("store")).build()
    store
  } else store

  private[this] def getOrCreateCollie(): ClusterCollie = if (clusterCollie == null) {
    this.clusterCollie =
      if (k8sClient == null) ClusterCollie.builderOfSsh().nodeCollie(createCollie()).build()
      else ClusterCollie.builderOfK8s().nodeCollie(createCollie()).k8sClient(k8sClient).build()
    clusterCollie
  } else clusterCollie
}
