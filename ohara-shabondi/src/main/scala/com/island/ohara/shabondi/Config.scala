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

private[shabondi] object Config {
  def apply(raw: Map[String, String]) =
    new Config(raw)
}

private[shabondi] class Config(raw: Map[String, String]) {
  import DefaultDefinitions._

  def serverType: String = raw(SERVER_TYPE_KEY)

  def port: Int = raw(CLIENT_PORT_KEY).toInt

  def sourceToTopics: Seq[TopicKey] = TopicKey.toTopicKeys(raw(SOURCE_TO_TOPICS_KEY)).asScala

  def sinksFromTopics: Seq[TopicKey] = TopicKey.toTopicKeys(raw(SINK_FROM_TOPICS_KEY)).asScala

  def brokers: String = raw(BROKERS_KEY)
}

object DefaultDefinitions {
  import scala.collection.JavaConverters._

  private val defaultDefinitions = mutable.Map.empty[String, SettingDef]
  private val orderCounter       = new AtomicInteger(0)
  private val coreGroup          = "core"

  private def orderNumber = orderCounter.getAndIncrement

  val SERVER_TYPE_SOURCE = "source"
  val SERVER_TYPE_SINK   = "sink"

  def all: Map[String, SettingDef] = defaultDefinitions.toMap

  val SERVER_TYPE_KEY = "shabondi.serverType"
  val SERVER_TYPE_DEFINITION = SettingDef.builder
    .key(SERVER_TYPE_KEY)
    .group(coreGroup)
    .orderInGroup(orderNumber)
    .optional(SERVER_TYPE_SOURCE, Set(SERVER_TYPE_SOURCE, SERVER_TYPE_SINK).asJava)
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

  implicit private class RegisterSettingDef(settingDef: SettingDef) {
    def registerDefault: SettingDef = {
      defaultDefinitions += (settingDef.key -> settingDef)
      settingDef
    }
  }
}
