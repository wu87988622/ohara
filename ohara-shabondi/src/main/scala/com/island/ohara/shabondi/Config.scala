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

package com.island.ohara.shabondi

import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.common.setting.SettingDef.Type
import com.island.ohara.common.setting.{SettingDef, TopicKey}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration._

private[shabondi] object Config {
  def apply(raw: Map[String, String]) =
    new Config(raw)
}

private[shabondi] class Config(raw: Map[String, String]) {
  import DefaultDefinitions._

  def serverType: String = raw(SERVER_TYPE_KEY)

  def port: Int = raw(CLIENT_PORT_KEY).toInt

  def sourceToTopics: Seq[TopicKey] = TopicKey.toTopicKeys(raw(SOURCE_TO_TOPICS_KEY)).asScala

  def sinkFromTopics: Seq[TopicKey] = TopicKey.toTopicKeys(raw(SINK_FROM_TOPICS_KEY)).asScala

  def sinkPollRowSize: Int = intValue(SINK_POLL_ROW_SIZE_DEF)

  def sinkPollTimeout: FiniteDuration = FiniteDuration(longValue(SINK_POLL_TIMEOUT_DEF), MILLISECONDS)

  def brokers: String = raw(BROKERS_KEY)

  private def longValue(settingDef: SettingDef): Long =
    if (!raw.contains(settingDef.key)) settingDef.defaultLong() else raw(settingDef.key).toLong

  private def intValue(settingDef: SettingDef): Int =
    if (!raw.contains(settingDef.key)) settingDef.defaultInt() else raw(settingDef.key).toInt
}

object DefaultDefinitions {
  import scala.collection.JavaConverters._

  private val defaultDefinitions = mutable.Map.empty[String, SettingDef]
  private val orderCounter       = new AtomicInteger(0)
  private val coreGroup          = "core"

  private def orderNumber = orderCounter.getAndIncrement

  val SERVER_TYPE_SOURCE = "source"
  val SERVER_TYPE_SINK   = "sink"
  val SERVER_TYPES       = Set(SERVER_TYPE_SOURCE, SERVER_TYPE_SINK)

  def all: Map[String, SettingDef] = defaultDefinitions.toMap

  val SERVER_TYPE_KEY = "shabondi.serverType"
  val SERVER_TYPE_DEFINITION = SettingDef.builder
    .key(SERVER_TYPE_KEY)
    .group(coreGroup)
    .orderInGroup(orderNumber)
    .optional(SERVER_TYPE_SOURCE, SERVER_TYPES.asJava)
    .displayName("Shabondi server type")
    .documentation("The server type when Shabondi service start.")
    .build
    .registerDefault

  val CLIENT_PORT_KEY = "shabondi.client.port"
  val CLIENT_PORT_DEFINITION = SettingDef.builder
    .key(CLIENT_PORT_KEY)
    .group(coreGroup)
    .orderInGroup(orderNumber)
    .required(Type.BINDING_PORT)
    .displayName("Topic topic of data produce to")
    .build
    .registerDefault

  val BROKERS_KEY = "shabondi.brokers"
  val BROKERS_DEFINITION = SettingDef.builder
    .key(BROKERS_KEY)
    .group(coreGroup)
    .orderInGroup(orderNumber)
    .required(Type.STRING)
    .displayName("Broker list")
    .documentation("The broker list of current workspace")
    .build
    .registerDefault

  val SOURCE_TO_TOPICS_KEY = "shabondi.source.toTopics"
  val SOURCE_TO_TOPICS_DEFINITION = SettingDef.builder
    .key(SOURCE_TO_TOPICS_KEY)
    .group(coreGroup)
    .orderInGroup(orderNumber)
    .reference(SettingDef.Reference.TOPIC)
    .displayName("Target topic")
    .documentation("The topic that Shabondi will push rows into")
    .optional(Type.OBJECT_KEYS)
    .build
    .registerDefault

  val SINK_FROM_TOPICS_KEY = "shabondi.sink.fromTopics"
  val SINK_FROM_TOPICS_DEFINITION = SettingDef.builder
    .key(SINK_FROM_TOPICS_KEY)
    .group(coreGroup)
    .orderInGroup(orderNumber)
    .reference(SettingDef.Reference.TOPIC)
    .displayName("Source topic")
    .documentation("The topic that Shabondi will pull rows from")
    .optional(Type.OBJECT_KEYS)
    .build
    .registerDefault

  val SINK_POLL_ROW_SIZE = "shabondi.sink.poll.rowsize"
  val SINK_POLL_ROW_SIZE_DEF = SettingDef.builder
    .key(SINK_POLL_ROW_SIZE)
    .group(coreGroup)
    .orderInGroup(orderNumber)
    .positiveNumber(500)
    .displayName("Poll row size")
    .documentation("The row size that each poll from topic")
    .build
    .registerDefault

  val SINK_POLL_TIMEOUT = "shabondi.sink.poll.timeout"
  val SINK_POLL_TIMEOUT_DEF = SettingDef.builder
    .key(SINK_POLL_TIMEOUT)
    .group(coreGroup)
    .orderInGroup(orderNumber)
    .positiveNumber(500L)
    .displayName("Poll timeout")
    .documentation("The timeout value(milliseconds) that each poll from topic")
    .build
    .registerDefault

  implicit private class RegisterSettingDef(settingDef: SettingDef) {
    def registerDefault: SettingDef = {
      defaultDefinitions += (settingDef.key -> settingDef)
      settingDef
    }
  }
}
