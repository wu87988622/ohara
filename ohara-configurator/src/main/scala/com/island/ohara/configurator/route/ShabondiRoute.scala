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

package com.island.ohara.configurator.route

import akka.http.scaladsl.server
import com.island.ohara.agent.{ServiceCollie, ShabondiCollie}
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.{ShabondiApi}
import com.island.ohara.common.setting.{ObjectKey, SettingDef}
import com.island.ohara.common.setting.SettingDef.Necessary
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.ObjectChecker.Condition.{RUNNING, STOPPED}
import com.island.ohara.configurator.route.hook._
import com.island.ohara.configurator.store.{DataStore, MeterCache}
import spray.json.{JsString, JsValue}

import scala.concurrent.{ExecutionContext, Future}

private[configurator] object ShabondiRoute {
  import com.island.ohara.shabondi.DefaultDefinitions._
  import ShabondiApi._

  private[route] def necessaryContains(definition: SettingDef, settings: Map[String, JsValue]): Unit = {
    if (definition.necessary() == Necessary.REQUIRED) {
      if (!definition.recommendedValues.isEmpty) {
        val value = settings(definition.key()).asInstanceOf[JsString].value
        if (!definition.recommendedValues.contains(value))
          throw new IllegalArgumentException(
            s"Invalid value of ${definition.key}, must be one of ${definition.recommendedValues}"
          )
      }
    }
  }

  private[this] def creationToClusterInfo(creation: ShabondiClusterCreation)(
    implicit objectChecker: ObjectChecker,
    executionContext: ExecutionContext
  ): Future[ShabondiClusterInfo] = {
    necessaryContains(SERVER_TYPE_DEFINITION, creation.settings)
    val serverType = creation.serverType
    objectChecker.checkList
      .nodeNames(creation.nodeNames)
      .brokerCluster(creation.brokerClusterKey)
      .topics {
        serverType match {
          case SERVER_TYPE_SOURCE => creation.sourceToTopics
          case SERVER_TYPE_SINK   => creation.sinkFromTopics
        }
      }
      .check()
      .map { _: ObjectChecker.ObjectInfos =>
        val settings = ShabondiApi.access.request.settings(creation.settings).creation.settings
        ShabondiClusterInfo(
          settings = settings,
          aliveNodes = Set.empty,
          state = None,
          metrics = Metrics(Seq.empty),
          error = None,
          lastModified = CommonUtils.current()
        )
      }
  }

  private[this] def hookOfCreation(
    implicit objectChecker: ObjectChecker,
    executionContext: ExecutionContext
  ): HookOfCreation[ShabondiClusterCreation, ShabondiClusterInfo] =
    creationToClusterInfo(_)

  private[this] def hookOfUpdating(
    implicit objectChecker: ObjectChecker,
    executionContext: ExecutionContext
  ): HookOfUpdating[ShabondiClusterUpdating, ShabondiClusterInfo] =
    (key: ObjectKey, updating: ShabondiClusterUpdating, previousOption: Option[ShabondiClusterInfo]) =>
      previousOption match {
        case None =>
          val creation = ShabondiApi.access.request
            .settings(updating.settings)
            .key(key)
            .creation
          creationToClusterInfo(creation)
        case Some(previous) =>
          objectChecker.checkList
            .check()
            .flatMap { _ =>
              val creation = ShabondiApi.access.request
                .settings(previous.settings)
                .settings(keepEditableFields(updating.settings, ShabondiApi.DEFINITIONS))
                .key(key)
                .creation
              creationToClusterInfo(creation)
            }
      }

  private[this] def hookOfStart(
    implicit objectChecker: ObjectChecker,
    shabondiCollie: ShabondiCollie,
    executionContext: ExecutionContext
  ): HookOfAction[ShabondiClusterInfo] =
    (clusterInfo: ShabondiClusterInfo, subName: String, params: Map[String, String]) => {
      val checkTopics = clusterInfo.serverType match {
        case SERVER_TYPE_SOURCE => clusterInfo.sourceToTopics
        case SERVER_TYPE_SINK   => clusterInfo.sinkFromTopics
      }
      if (checkTopics.isEmpty) {
        val key = clusterInfo.serverType match {
          case SERVER_TYPE_SOURCE => SOURCE_TO_TOPICS_DEFINITION.key
          case SERVER_TYPE_SINK   => SINK_FROM_TOPICS_DEFINITION.key
        }
        throw new IllegalArgumentException(s"$key cannot be empty.")
      }
      objectChecker.checkList
        .shabondi(clusterInfo.key)
        .brokerCluster(clusterInfo.brokerClusterKey, RUNNING)
        .topics(checkTopics, RUNNING)
        .check()
        .flatMap { objInfo: ObjectChecker.ObjectInfos =>
          val condition = objInfo.shabondiClusterInfos.head._2
          condition match {
            case RUNNING => Future.unit
            case STOPPED =>
              val brokerClusterInfo = objInfo.brokerClusterInfos.head._1
              shabondiCollie.creator
                .settings(clusterInfo.settings)
                .name(clusterInfo.name)
                .group(clusterInfo.group)
                .nodeNames(clusterInfo.nodeNames)
                .brokerClusterKey(brokerClusterInfo.key)
                .brokers(brokerClusterInfo.connectionProps)
                .threadPool(executionContext)
                .create()
          }
        }
    }

  private[this] def hookBeforeStop(): HookOfAction[ShabondiClusterInfo] = (_, _, _) => Future.unit

  private[this] def hookBeforeDelete(): HookBeforeDelete = _ => Future.unit

  def apply(
    implicit store: DataStore,
    objectChecker: ObjectChecker,
    shabondiCollie: ShabondiCollie,
    serviceCollie: ServiceCollie,
    meterCache: MeterCache,
    executionContext: ExecutionContext
  ): server.Route = {
    clusterRoute[ShabondiClusterInfo, ShabondiClusterCreation, ShabondiClusterUpdating](
      root = SHABONDI_PREFIX_PATH,
      metricsKey = Some("shabondi"),
      hookOfCreation = hookOfCreation,
      hookOfUpdating = hookOfUpdating,
      hookOfStart = hookOfStart,
      hookBeforeStop = hookBeforeStop,
      hookBeforeDelete = hookBeforeDelete
    )
  }
}
