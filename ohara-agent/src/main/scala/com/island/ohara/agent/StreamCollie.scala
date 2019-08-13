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

import java.net.URL
import java.util.Objects

import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, PortMapping, PortPair}
import com.island.ohara.client.configurator.v0.FileInfoApi.{FILE_INFO_JSON_FORMAT, FileInfo}
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.client.configurator.v0.{ClusterInfo, Definition, StreamApi}
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.metrics.BeanChannel
import com.island.ohara.metrics.basic.CounterMBean
import com.island.ohara.streams.config.StreamDefinitions
import com.typesafe.scalalogging.Logger
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
  * An interface of controlling stream cluster.
  * It isolates the implementation of container manager from Configurator.
  */
trait StreamCollie extends Collie[StreamClusterInfo, StreamCollie.ClusterCreator] {
  private[this] val LOG = Logger(classOf[StreamCollie])

  override def creator: StreamCollie.ClusterCreator =
    (clusterName, nodeNames, imageName, jarInfo, jmxPort, _, _, settings, executionContext) => {
      implicit val exec: ExecutionContext = executionContext
      clusters().flatMap(clusters => {
        if (clusters.keys.filter(_.isInstanceOf[StreamClusterInfo]).exists(_.name == clusterName))
          Future.failed(new IllegalArgumentException(s"stream cluster:$clusterName exists!"))
        else
          nodeCollie
            .nodes(nodeNames)
            .map(_.map(node => node -> ContainerCollie.format(prefixKey, clusterName, serviceName)).toMap)
            .flatMap {
              nodes =>
                val route: Map[String, String] = nodes.keys.map { node =>
                  node.hostname -> CommonUtils.address(node.hostname)
                }.toMap +
                  // make sure the streamApp can connect to configurator
                  (jarInfo.url.getHost -> CommonUtils.address(jarInfo.url.getHost))
                // ssh connection is slow so we submit request by multi-thread
                Future
                  .sequence(nodes.map {
                    case (node, containerName) =>
                      val containerInfo = ContainerInfo(
                        nodeName = node.name,
                        id = ContainerCollie.UNKNOWN,
                        imageName = imageName,
                        created = ContainerCollie.UNKNOWN,
                        state = ContainerCollie.UNKNOWN,
                        kind = ContainerCollie.UNKNOWN,
                        name = containerName,
                        size = ContainerCollie.UNKNOWN,
                        portMappings = Seq(
                          PortMapping(
                            hostIp = ContainerCollie.UNKNOWN,
                            portPairs = Seq(
                              PortPair(
                                hostPort = jmxPort,
                                containerPort = jmxPort
                              )
                            )
                          )
                        ),
                        environments = settings.map {
                          case (k, v) =>
                            k -> (v match {
                              case JsString(value) => value
                              // TODO: discard the js array? by chia
                              case JsArray(arr) =>
                                arr.map(_.convertTo[String]).mkString(",")
                              case _ => v.toString()
                            })
                        }
                        // we convert all settings to specific string in order to fetch all settings from
                        // container env quickly. Also, the specific string enable us to pick up the "true" settings
                        // from envs since there are many system-defined settings in container envs.
                          ++ toEnvString(settings),
                        // we should set the hostname to container name in order to avoid duplicate name with other containers
                        hostname = containerName
                      )
                      doCreator(executionContext, containerName, containerInfo, node, route, jmxPort, jarInfo).map(_ =>
                        Some(containerInfo))
                  })
                  .map(_.flatten.toSeq)
                  .flatMap(cs => loadDefinition(jarInfo.url).map(definition => cs -> definition))
                  .map {
                    case (successfulContainers, definition) =>
                      if (successfulContainers.isEmpty)
                        throw new IllegalArgumentException(s"failed to create $clusterName on $serviceName")
                      val clusterInfo = StreamClusterInfo(
                        // the other arguments (clusterName, imageName and so on) are extracted from settings so
                        // we don't need to add them back to settings.
                        settings = settings,
                        // TODO: cluster info
                        definition = definition,
                        nodeNames = successfulContainers.map(_.nodeName).toSet,
                        deadNodes = Set.empty,
                        metrics = Metrics(Seq.empty),
                        // creating cluster success but still need to update state by another request
                        state = None,
                        error = None,
                        lastModified = CommonUtils.current()
                      )
                      postCreateCluster(clusterInfo, successfulContainers)
                      clusterInfo
                  }
            }
      })
    }

  /**
    * Get all counter beans from cluster
    * @param cluster cluster
    * @return counter beans
    */
  def counters(cluster: StreamClusterInfo): Seq[CounterMBean] = cluster.nodeNames.flatMap { node =>
    BeanChannel.builder().hostname(node).port(cluster.jmxPort).build().counterMBeans().asScala
  }.toSeq

  /**
    *
    * @return async future containing configs
    */
  /**
    * Get all '''SettingDef''' of current streamApp.
    * Note: This method intends to call a method that invokes the reflection method of streamApp.
    *
    * @param jarUrl the custom streamApp jar url
    * @return stream definition
    */
  //TODO : this workaround should be removed and use a new API instead in #2191...by Sam
  def loadDefinition(jarUrl: URL)(implicit executionContext: ExecutionContext): Future[Option[Definition]] =
    Future {
      import sys.process._
      val classpath = System.getProperty("java.class.path")
      val command =
        s"""java -cp "$classpath" ${StreamCollie.MAIN_ENTRY} ${StreamDefinitions.JAR_KEY_DEFINITION
          .key()}=${jarUrl.toString} ${StreamCollie.CONFIG_KEY}"""
      val result = command.!!
      val className = result.split("=")(0)
      Some(Definition(className, StreamDefinitions.ofJson(result.split("=")(1)).values().asScala))
    }.recover {
      case e: RuntimeException =>
        // We cannot parse the provided jar, return nothing and log it
        LOG.warn(s"the provided jar url: [$jarUrl] could not be parsed, return default settings only.", e)
        None
    }

  private[agent] def toStreamCluster(clusterName: String, containers: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[StreamClusterInfo] = {
    // get the first running container, or first non-running container if not found
    val first = containers.find(_.state == ContainerState.RUNNING.name).getOrElse(containers.head)
    val settings = toSettings(first.environments)
    val jarInfo = FILE_INFO_JSON_FORMAT.read(settings(StreamDefinitions.JAR_INFO_DEFINITION.key()))
    loadDefinition(jarInfo.url).map { definition =>
      StreamClusterInfo(
        settings = settings,
        // we don't care the runtime definitions; it's saved to store already
        definition = definition,
        nodeNames = containers.map(_.nodeName).toSet,
        // Currently, docker and k8s has same naming rule for "Running",
        // it is ok that we use the containerState.RUNNING here.
        deadNodes = containers.filterNot(_.state == ContainerState.RUNNING.name).map(_.nodeName).toSet,
        metrics = Metrics(Seq.empty),
        state = {
          // we only have two possible results here:
          // 1. only assume cluster is "running" if at least one container is running
          // 2. the cluster state is always "dead" if all containers were not running
          val alive = containers.exists(_.state == ContainerState.RUNNING.name)
          if (alive) Some(ClusterState.RUNNING.name) else Some(ClusterState.FAILED.name)
        },
        error = None,
        lastModified = CommonUtils.current()
      )
    }
  }

  /**
    * Define nodeCollie by different environment
    * @return
    */
  protected def nodeCollie: NodeCollie

  /**
    * Define prefixKey by different environment
    * @return prefix key
    */
  protected def prefixKey: String

  /**
    * Define serviceName by different environment
    * @return service name
    */
  protected def serviceName: String

  protected def doCreator(executionContext: ExecutionContext,
                          containerName: String,
                          containerInfo: ContainerInfo,
                          node: Node,
                          route: Map[String, String],
                          jmxPort: Int,
                          jarInfo: FileInfo): Future[Unit]

  protected def postCreateCluster(clusterInfo: ClusterInfo, successfulContainers: Seq[ContainerInfo]): Unit = {
    //Default do nothing
  }
}

object StreamCollie {
  trait ClusterCreator extends Collie.ClusterCreator[StreamClusterInfo] {
    private[this] var jarInfo: FileInfo = _
    private[this] val settings: mutable.Map[String, JsValue] = mutable.Map[String, JsValue]()
    override protected def doCopy(clusterInfo: StreamClusterInfo): Unit = {
      // doCopy is used to add node for a running cluster.
      // Currently, StreamClusterInfo does not carry enough information to be copied and it is unsupported to add a node to a running streamapp cluster
      // Hence, it is fine to do nothing here
      // TODO: fill the correct implementation if we support to add node to a running streamapp cluster.
    }

    /**
      * Stream collie overrides this method in order to put the same config to settings.
      * @param clusterName cluster name
      * @return this creator
      */
    override def clusterName(clusterName: String): ClusterCreator.this.type = {
      super.clusterName(clusterName)
      settings += StreamDefinitions.NAME_DEFINITION.key() -> JsString(clusterName)
      this
    }

    /**
      * Stream collie overrides this method in order to put the same config to settings.
      * @param nodeNames nodes' name
      * @return cluster description
      */
    override def nodeNames(nodeNames: Set[String]): ClusterCreator.this.type = {
      super.nodeNames(nodeNames)
      settings += StreamDefinitions.NODE_NAMES_DEFINITION.key() -> JsArray(nodeNames.map(JsString(_)).toVector)
      this
    }

    /**
      * Stream collie overrides this method in order to put the same config to settings.
      * @param imageName image name
      * @return this creator
      */
    override def imageName(imageName: String): ClusterCreator.this.type = {
      super.imageName(imageName)
      settings += StreamDefinitions.IMAGE_NAME_DEFINITION.key() -> JsString(imageName)
      this
    }

    /**
      * set the jar url for the streamApp running
      *
      * @param jarInfo jar info
      * @return this creator
      */
    def jarInfo(jarInfo: FileInfo): ClusterCreator = {
      this.jarInfo = Objects.requireNonNull(jarInfo)
      import spray.json._
      settings += StreamDefinitions.JAR_KEY_DEFINITION.key() -> ObjectKey.toJsonString(jarInfo.key).parseJson
      settings += StreamDefinitions.JAR_INFO_DEFINITION.key() -> FILE_INFO_JSON_FORMAT.write(jarInfo)
      this
    }

    /**
      * add a key-value data map for container
      *
      * @param key data key
      * @param value data value
      * @return this creator
      */
    @Optional("default is empty map")
    def setting(key: String, value: JsValue): ClusterCreator = settings(Map(key -> value))

    /**
      * add the key-value data map for container
      *
      * @param settings data settings
      * @return this creator
      */
    @Optional("default is empty map")
    def settings(settings: Map[String, JsValue]): ClusterCreator = {
      this.settings ++= Objects.requireNonNull(settings)
      this
    }

    /**
      * get the value, which is mapped to input key, from settings. If the key is not associated, NoSuchElementException will
      * be thrown.
      * @param key key
      * @param f marshalling function
      * @tparam T type
      * @return value
      */
    private[this] def get[T](key: String, f: JsValue => T): T =
      settings.get(key).map(f).getOrElse(throw new NoSuchElementException(s"$key is required"))

    override def create(): Future[StreamClusterInfo] = doCreate(
      clusterName = get(StreamDefinitions.NAME_DEFINITION.key(), _.convertTo[String]),
      nodeNames = get(StreamDefinitions.NODE_NAMES_DEFINITION.key(), _.convertTo[Vector[String]].toSet),
      imageName = get(StreamDefinitions.IMAGE_NAME_DEFINITION.key(), _.convertTo[String]),
      jarInfo = Objects.requireNonNull(jarInfo),
      jmxPort = get(StreamDefinitions.JMX_PORT_DEFINITION.key(), _.convertTo[Int]),
      fromTopics = CommonUtils
        .requireNonEmpty(
          get(StreamDefinitions.FROM_TOPICS_DEFINITION.key(), _.convertTo[Set[String]]).asJava,
          () => s"${StreamDefinitions.FROM_TOPICS_DEFINITION.key()} can't be associated to empty array"
        )
        .asScala
        .toSet,
      toTopics = CommonUtils
        .requireNonEmpty(
          get(StreamDefinitions.TO_TOPICS_DEFINITION.key(), _.convertTo[Set[String]]).asJava,
          () => s"${StreamDefinitions.TO_TOPICS_DEFINITION.key()} can't be associated to empty array"
        )
        .asScala
        .toSet,
      settings = Objects.requireNonNull(settings.toMap),
      executionContext = Objects.requireNonNull(executionContext)
    )

    override protected def checkClusterName(clusterName: String): String = {
      StreamApi.STREAM_CREATION_JSON_FORMAT.check("name", JsString(clusterName))
      clusterName
    }

    protected def doCreate(clusterName: String,
                           nodeNames: Set[String],
                           imageName: String,
                           jarInfo: FileInfo,
                           jmxPort: Int,
                           fromTopics: Set[String],
                           toTopics: Set[String],
                           settings: Map[String, JsValue],
                           executionContext: ExecutionContext): Future[StreamClusterInfo]
  }

  /**
    * the only entry for ohara streamApp
    */
  val MAIN_ENTRY = "com.island.ohara.streams.StreamApp"

  /**
    * the flag to get/set streamApp configs for container
    */
  private[agent] val CONFIG_KEY = "CONFIG_KEY"

  /**
    * generate the jmx required properties
    *
    * @param hostname the hostname used by jmx remote
    * @param port the port used by jmx remote
    * @return jmx properties
    */
  private[agent] def formatJMXProperties(hostname: String, port: Int): Seq[String] = {
    Seq(
      "-Dcom.sun.management.jmxremote",
      "-Dcom.sun.management.jmxremote.authenticate=false",
      "-Dcom.sun.management.jmxremote.ssl=false",
      s"-Dcom.sun.management.jmxremote.port=$port",
      s"-Dcom.sun.management.jmxremote.rmi.port=$port",
      s"-Djava.rmi.server.hostname=$hostname"
    )
  }
}
