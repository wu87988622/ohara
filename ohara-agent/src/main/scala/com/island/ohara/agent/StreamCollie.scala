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

import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.metrics.BeanChannel
import com.island.ohara.metrics.basic.CounterMBean

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

/**
  * An interface of controlling stream cluster.
  * It isolates the implementation of container manager from Configurator.
  */
trait StreamCollie extends Collie[StreamClusterInfo, StreamCollie.ClusterCreator] {

  /**
    * Get all counter beans from cluster
    * @param cluster cluster
    * @return counter beans
    */
  def counters(cluster: StreamClusterInfo): Seq[CounterMBean] = cluster.nodeNames.flatMap { node =>
    BeanChannel.builder().hostname(node).port(cluster.jmxPort).build().counterMBeans().asScala
  }.toSeq

  private[agent] def toStreamCluster(clusterName: String, containers: Seq[ContainerInfo]): Future[StreamClusterInfo] = {
    // get the first running container, or first non-running container if not found
    val first = containers.find(_.state == ContainerState.RUNNING.name).getOrElse(containers.head)
    Future.successful(
      StreamClusterInfo(
        name = clusterName,
        imageName = first.imageName,
        nodeNames = containers.map(_.nodeName).toSet,
        // Currently, docker and k8s has same naming rule for "Running",
        // it is ok that we use the containerState.RUNNING here.
        deadNodes = containers.filterNot(_.state == ContainerState.RUNNING.name).map(_.nodeName).toSet,
        // Currently, streamApp use expose portMappings for jmx port only.
        // Since dead container would not expose the port, we directly get it from environment for consistency.
        jmxPort = first.environments(StreamCollie.JMX_PORT_KEY).toInt,
        state = {
          // we only have two possible results here:
          // 1. only assume cluster is "running" if at least one container is running
          // 2. the cluster state is always "dead" if all containers were not running
          val alive = containers.exists(_.state == ContainerState.RUNNING.name)
          if (alive) Some(ContainerState.RUNNING.name) else Some(ContainerState.DEAD.name)
        }
      )
    )
  }
}

object StreamCollie {
  trait ClusterCreator extends Collie.ClusterCreator[StreamClusterInfo] {
    private[this] var jarUrl: String = _
    private[this] var appId: String = _
    private[this] var brokerProps: String = _
    private[this] var fromTopics: Set[String] = Set.empty
    private[this] var toTopics: Set[String] = Set.empty
    private[this] var jmxPort: Int = CommonUtils.availablePort()
    private[this] var exactlyOnce: Boolean = false

    override protected def doCopy(clusterInfo: StreamClusterInfo): Unit = {
      // doCopy is used to add node for a running cluster.
      // Currently, StreamClusterInfo does not carry enough information to be copied and it is unsupported to add a node to a running streamapp cluster
      // Hence, it is fine to do nothing here
      // TODO: fill the correct implementation if we support to add node to a running streamapp cluster.
    }

    /**
      * set the jar url for the streamApp running
      *
      * @param jarUrl jar url
      * @return this creator
      */
    def jarUrl(jarUrl: String): ClusterCreator = {
      this.jarUrl = CommonUtils.requireNonEmpty(jarUrl)
      this
    }

    /**
      * set the appId for the streamApp
      * NOTED: this appId should be unique from other streamApps
      *
      * @param appId app id
      * @return this creator
      */
    def appId(appId: String): ClusterCreator = {
      this.appId = CommonUtils.requireNonEmpty(appId)
      this
    }

    /**
      * set the broker connection props (host:port,...)
      *
      * @param brokerProps broker props
      * @return this creator
      */
    def brokerProps(brokerProps: String): ClusterCreator = {
      this.brokerProps = CommonUtils.requireNonEmpty(brokerProps)
      this
    }

    /**
      * set the topics that the streamApp consumed with
      *
      * @param fromTopics from topics
      * @return this creator
      */
    def fromTopics(fromTopics: Set[String]): ClusterCreator = {
      this.fromTopics = CommonUtils.requireNonEmpty(fromTopics.asJava).asScala.toSet
      this
    }

    /**
      * set the topics that the streamApp produced to
      *
      * @param toTopics to topics
      * @return this creator
      */
    def toTopics(toTopics: Set[String]): ClusterCreator = {
      this.toTopics = CommonUtils.requireNonEmpty(toTopics.asJava).asScala.toSet
      this
    }

    /**
      * set the jmx port
      *
      * @param jmxPort jmx port
      * @return this creator
      */
    @Optional("default is local random port")
    def jmxPort(jmxPort: Int): ClusterCreator = {
      this.jmxPort = CommonUtils.requireConnectionPort(jmxPort)
      this
    }

    /**
      * set whether enable exactly once
      *
      * @param exactlyOnce exactlyOnce
      * @return this creator
      */
    @Optional("default is false")
    def enableExactlyOnce(exactlyOnce: Boolean): ClusterCreator = {
      this exactlyOnce = Objects.requireNonNull(exactlyOnce)
      this
    }

    override def create()(implicit executionContext: ExecutionContext): Future[StreamClusterInfo] = doCreate(
      CommonUtils.requireNonEmpty(clusterName),
      CommonUtils.requireNonEmpty(nodeNames.asJava).asScala.toSet,
      CommonUtils.requireNonEmpty(imageName),
      CommonUtils.requireNonEmpty(jarUrl),
      CommonUtils.requireNonEmpty(appId),
      CommonUtils.requireNonEmpty(brokerProps),
      CommonUtils.requireNonEmpty(fromTopics.asJava).asScala.toSet,
      CommonUtils.requireNonEmpty(toTopics.asJava).asScala.toSet,
      CommonUtils.requireConnectionPort(jmxPort),
      Objects.requireNonNull(exactlyOnce),
      Objects.requireNonNull(executionContext)
    )

    protected def doCreate(clusterName: String,
                           nodeNames: Set[String],
                           imageName: String,
                           jarUrl: String,
                           appId: String,
                           brokerProps: String,
                           fromTopics: Set[String],
                           toTopics: Set[String],
                           jmxPort: Int,
                           enableExactlyOnce: Boolean,
                           executionContext: ExecutionContext): Future[StreamClusterInfo]
  }

  private[agent] val JARURL_KEY: String = "STREAMAPP_JARURL"
  private[agent] val APPID_KEY: String = "STREAMAPP_APPID"
  private[agent] val SERVERS_KEY: String = "STREAMAPP_SERVERS"
  private[agent] val FROM_TOPIC_KEY: String = "STREAMAPP_FROMTOPIC"
  private[agent] val TO_TOPIC_KEY: String = "STREAMAPP_TOTOPIC"
  private[agent] val JMX_PORT_KEY: String = "STREAMAPP_JMX_PORT"
  private[agent] val EXACTLY_ONCE: String = "STREAMAPP_EXACTLY_ONCE"

  /**
    * the only entry for ohara streamApp
    */
  private[agent] val MAIN_ENTRY = "com.island.ohara.streams.StreamApp"

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
