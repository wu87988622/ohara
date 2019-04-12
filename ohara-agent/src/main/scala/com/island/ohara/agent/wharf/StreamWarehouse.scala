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

package com.island.ohara.agent.wharf

import java.util.Objects

import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.CommonUtils

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

trait StreamWarehouse extends Warehouse[StreamClusterInfo] {
  override def creator(): StreamWarehouse.StreamCreator
}

private[agent] object StreamWarehouse {
  final val STREAM_SERVICE_NAME: String = "stream"

  trait StreamCreator extends Warehouse.WarehouseCreator[StreamClusterInfo] {

    private[this] var jarUrl: String = _
    private[this] var instance: Int = 0
    private[this] var appId: String = _
    private[this] var brokerProps: String = _
    private[this] var fromTopics: Seq[String] = Seq.empty
    private[this] var toTopics: Seq[String] = Seq.empty
    private[this] var nodeNames: Seq[String] = _

    /**
      * set the jar url for the streamApp running
      *
      * @param jarUrl jar url
      * @return this creator
      */
    def jarUrl(jarUrl: String): StreamCreator = {
      this.jarUrl = CommonUtils.requireNonEmpty(jarUrl)
      this
    }

    /**
      * set the running instance for the streamApp
      * NOTED: do not set this value if you had set the nodeNames
      *
      * @param instance number of instance
      * @return the creator
      */
    @Optional("you can ignore this parameter if set nodeNames")
    def instance(instance: Int): StreamCreator = {
      this.instance = instance
      this
    }

    /**
      *  set nodes for cluster
      *  NOTED: this is a async method since starting a cluster is always gradual.
      *  ANOTHER NOTED: this value will override the effects of `instance`
      *
      * @param nodeNames nodes' name
      * @return this creator
      */
    def nodeNames(nodeNames: Seq[String]): StreamCreator = {
      this.nodeNames = CommonUtils.requireNonEmpty(nodeNames.asJava).asScala
      this
    }

    /**
      * set the appId for the streamApp
      * NOTED: this appId should be unique from other streamApps
      *
      * @param appId app id
      * @return this creator
      */
    def appId(appId: String): StreamCreator = {
      this.appId = CommonUtils.requireNonEmpty(appId)
      this
    }

    /**
      * set the broker connection props (host:port,...)
      *
      * @param brokerProps broker props
      * @return this creator
      */
    def brokerProps(brokerProps: String): StreamCreator = {
      this.brokerProps = CommonUtils.requireNonEmpty(brokerProps)
      this
    }

    /**
      * set the topics that the streamApp consumed with
      *
      * @param fromTopics from topics
      * @return this creator
      */
    def fromTopics(fromTopics: Seq[String]): StreamCreator = {
      this.fromTopics = CommonUtils.requireNonEmpty(fromTopics.asJava).asScala
      this
    }

    /**
      * set the topics that the streamApp produced to
      *
      * @param toTopics to topics
      * @return this creator
      */
    def toTopics(toTopics: Seq[String]): StreamCreator = {
      this.toTopics = CommonUtils.requireNonEmpty(toTopics.asJava).asScala
      this
    }

    override def create()(implicit executionContext: ExecutionContext): Future[StreamClusterInfo] = doCreate(
      CommonUtils.requireNonEmpty(clusterName),
      // we check nodeNames in StreamWarehouseImpl
      nodeNames,
      CommonUtils.requireNonEmpty(imageName),
      CommonUtils.requireNonEmpty(jarUrl),
      // we check instance in StreamWarehouseImpl
      instance,
      CommonUtils.requireNonEmpty(appId),
      CommonUtils.requireNonEmpty(brokerProps),
      CommonUtils.requireNonEmpty(fromTopics.asJava).asScala,
      CommonUtils.requireNonEmpty(toTopics.asJava).asScala,
      Objects.requireNonNull(executionContext)
    )

    protected def doCreate(clusterName: String,
                           nodeNames: Seq[String],
                           imageName: String,
                           jarUrl: String,
                           instance: Int,
                           appId: String,
                           brokerProps: String,
                           fromTopics: Seq[String],
                           toTopics: Seq[String],
                           executionContext: ExecutionContext): Future[StreamClusterInfo]
  }
}
