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
import java.lang.reflect.Modifier
import java.net.{URL, URLClassLoader}
import java.util.Objects
import java.util.concurrent.{ExecutorService, Executors}

import com.island.ohara.agent.container.ContainerName
import com.island.ohara.agent.docker.ServiceCollieImpl
import com.island.ohara.agent.k8s.{K8SClient, K8SServiceCollieImpl}
import com.island.ohara.client.configurator.v0.ClusterStatus
import com.island.ohara.client.configurator.v0.FileInfoApi.ClassInfo
import com.island.ohara.client.configurator.v0.InspectApi.FileContent
import com.island.ohara.client.configurator.v0.NodeApi.{Node, Resource}
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.pattern.Builder
import com.island.ohara.common.util.Releasable
import com.island.ohara.kafka.connector.{RowSinkConnector, RowSourceConnector}
import com.island.ohara.streams.Stream
import com.typesafe.scalalogging.Logger
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.ConfigurationBuilder

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

/**
  * This is the top-of-the-range "collie". It maintains and organizes all collies.
  * Each getter should return new instance of collie since each collie has close() method.
  * However, it is ok to keep global instance of collie if they have dump close().
  * Currently, default implementation is based on ssh and docker command. It is simple but slow.
  */
abstract class ServiceCollie extends Releasable {
  /**
    * create a collie for zookeeper cluster
    * @return zookeeper collie
    */
  def zookeeperCollie: ZookeeperCollie

  /**
    * create a collie for broker cluster
    * @return broker collie
    */
  def brokerCollie: BrokerCollie

  /**
    * create a collie for worker cluster
    * @return worker collie
    */
  def workerCollie: WorkerCollie

  /**
    * create a collie for stream cluster
    * @return stream collie
    */
  def streamCollie: StreamCollie

  def shabondiCollie: ShabondiCollie

  /**
    * the default implementation is expensive!!! Please override this method if you are a good programmer.
    * @return a collection of all clusters
    */
  def clusters()(implicit executionContext: ExecutionContext): Future[Seq[ClusterStatus]] =
    for {
      zkMap     <- zookeeperCollie.clusters()
      bkMap     <- brokerCollie.clusters()
      wkMap     <- workerCollie.clusters()
      streamMap <- streamCollie.clusters()
    } yield zkMap ++ bkMap ++ wkMap ++ streamMap

  /**
    * list the docker images hosted by nodes
    * @return the images stored by each node
    */
  def imageNames()(implicit executionContext: ExecutionContext): Future[Map[Node, Seq[String]]]

  /**
    * Verify the node are available to be used in collie.
    * @param node validated node
    * @param executionContext thread pool
    * @return succeed report in string. Or try with exception
    */
  def verifyNode(node: Node)(implicit executionContext: ExecutionContext): Future[String]

  /**
    * list all containers from the hosted nodes
    * @param executionContext thread pool
    * @return active containers
    */
  def containerNames()(implicit executionContext: ExecutionContext): Future[Seq[ContainerName]]

  /**
    * get the log of specific container name
    * @param containerName container name
    * @param executionContext thread pool
    * @return log or NoSuchElementException
    */
  def log(containerName: String, sinceSeconds: Option[Long])(
    implicit executionContext: ExecutionContext
  ): Future[(ContainerName, String)]

  /**
    * Fetch the available hardware resources of hosted nodes.
    * Noted: the different collie may return different resources. The caller should NOT assume the content of the
    * resources.
    *
    * @param executionContext thread pool
    * @return hardware resources of all hosted nodes
    */
  def resources()(implicit executionContext: ExecutionContext): Future[Map[String, Seq[Resource]]]

  /**
    * load the connectors class and stream classes from specific urls
    * @param urls urls
    * @return (sources, sinks, streams)
    */
  def classNames(urls: Seq[URL]): ClassNames =
    classNames(new Reflections(new ConfigurationBuilder().addUrls(urls.asJava)))

  private[this] def classNames(reflections: Reflections): ClassNames = {
    def fetch(clz: Class[_]): Set[String] =
      // classOf[SubTypesScanner].getSimpleName is hard-code since Reflections does not expose it ...
      reflections.getStore.getAll(classOf[SubTypesScanner].getSimpleName, clz.getName).asScala.toSet
    new ClassNames(
      sources = fetch(classOf[RowSourceConnector]),
      sinks = fetch(classOf[RowSinkConnector]),
      streams = fetch(classOf[Stream])
    )
  }

  /**
    * load the definitions from input urls. Noted, the default implementation invokes an new class loader to resolve all
    * classes from input urls. If the input urls reference to non-uber jars, the CNF exception may happend and the classes
    * is skipped.
    * @param urls urls to load
    * @param executionContext thread pool
    * @return classes information
    */
  def fileContent(urls: Seq[URL])(implicit executionContext: ExecutionContext): Future[FileContent] = Future {
    val reflections =
      new Reflections(new ConfigurationBuilder().addUrls(urls.asJava).addClassLoader(new URLClassLoader(urls.toArray)))
    val expectedNames = classNames(reflections)
    if (expectedNames.all.isEmpty) FileContent.empty
    else {
      def seek[T](clz: Class[T]) =
        reflections
          .getSubTypesOf(clz)
          .asScala
          .filter(clz => expectedNames.all.contains(clz.getName))
          .filterNot(clz => Modifier.isAbstract(clz.getModifiers))

      val result = seek(classOf[RowSourceConnector]).flatMap { clz =>
        try Some(clz.getName -> clz.newInstance().settingDefinitions().asScala)
        catch {
          case e: Throwable =>
            ServiceCollie.LOG.warn(s"failed to load source class", e)
            None
        }
      } ++ seek(classOf[RowSinkConnector]).flatMap { clz =>
        try Some(clz.getName -> clz.newInstance().settingDefinitions().asScala)
        catch {
          case e: Throwable =>
            ServiceCollie.LOG.warn(s"failed to load sink class", e)
            None
        }
      } ++ seek(classOf[Stream]).flatMap { clz =>
        try Some(clz.getName -> clz.newInstance().definitions().asScala)
        catch {
          case e: Throwable =>
            ServiceCollie.LOG.warn(s"failed to load stream class", e)
            None
        }
      }
      FileContent(result.map {
        case (name, settingDefinitions) => ClassInfo(name, settingDefinitions)
      }.toSeq)
    }
  }
}

object ServiceCollie {
  val LOG = Logger(classOf[ServiceCollie])

  /**
    * the default implementation uses ssh and docker command to manage all clusters.
    * Each node running the service has name "{clusterName}-{service}-{index}".
    * For example, there is a worker cluster called "workercluster" and it is run on 3 nodes.
    * node-0 => workercluster-worker-0
    * node-1 => workercluster-worker-1
    * node-2 => workercluster-worker-2
    */
  def dockerModeBuilder: DockerModeBuilder = new DockerModeBuilder

  import scala.concurrent.duration._

  class DockerModeBuilder private[ServiceCollie] extends Builder[ServiceCollie] {
    private[this] var dataCollie: DataCollie           = _
    private[this] var cacheTimeout: Duration           = 3 seconds
    private[this] var cacheThreadPool: ExecutorService = _

    def dataCollie(dataCollie: DataCollie): DockerModeBuilder = {
      this.dataCollie = Objects.requireNonNull(dataCollie)
      this
    }

    @Optional("default is 3 seconds")
    def cacheTimeout(cacheTimeout: Duration): DockerModeBuilder = {
      this.cacheTimeout = Objects.requireNonNull(cacheTimeout)
      this
    }

    @Optional("The initial size of default pool is equal with number of cores")
    def cacheThreadPool(cacheThreadPool: ExecutorService): DockerModeBuilder = {
      this.cacheThreadPool = Objects.requireNonNull(cacheThreadPool)
      this
    }

    /**
      * We don't return ServiceCollieImpl since it is a private implementation
      * @return
      */
    override def build: ServiceCollie = new ServiceCollieImpl(
      cacheTimeout = Objects.requireNonNull(cacheTimeout),
      dataCollie = Objects.requireNonNull(dataCollie),
      cacheThreadPool =
        if (cacheThreadPool == null) Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
        else cacheThreadPool
    )
  }

  /**
    * Create a builder for instantiating k8s collie.
    * Currently, the nodes in node collie must be equal to nodes which is controllable to k8s client.
    * @return builder for k8s implementation
    */
  def k8sModeBuilder: K8sModeBuilder = new K8sModeBuilder

  class K8sModeBuilder private[ServiceCollie] extends Builder[ServiceCollie] {
    private[this] var dataCollie: DataCollie = _
    private[this] var k8sClient: K8SClient   = _

    def dataCollie(dataCollie: DataCollie): K8sModeBuilder = {
      this.dataCollie = Objects.requireNonNull(dataCollie)
      this
    }

    def k8sClient(k8sClient: K8SClient): K8sModeBuilder = {
      this.k8sClient = Objects.requireNonNull(k8sClient)
      this
    }

    /**
      * We don't return ServiceCollieImpl since it is a private implementation
      * @return
      */
    override def build: ServiceCollie = new K8SServiceCollieImpl(
      dataCollie = Objects.requireNonNull(dataCollie),
      k8sClient = Objects.requireNonNull(k8sClient)
    )
  }
}
