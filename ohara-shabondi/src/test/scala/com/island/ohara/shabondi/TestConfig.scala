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

import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.TopicKey
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.JavaConverters._

class TestConfig extends OharaTest with Matchers {
  import DefaultDefinitions._

  private def topicKey1 = TopicKey.of("default", "topic1")
  private def topicKey2 = TopicKey.of("default", "topic2")

  @Test
  def testConfig(): Unit = {
    val jsonSourceTopicKeys = TopicKey.toJsonString(Seq(topicKey1, topicKey2).asJava)
    val jsonSinkTopicKeys   = TopicKey.toJsonString(Seq(topicKey1, topicKey2).asJava)
    val args = Array(
      s"$SERVER_TYPE_KEY=source",
      s"$CLIENT_PORT_KEY=8080",
      s"$SOURCE_TO_TOPICS_KEY=$jsonSourceTopicKeys",
      s"$SINK_FROM_TOPICS_KEY=$jsonSinkTopicKeys"
    )

    val rawConfig = parseArgs(args)
    val config    = Config(rawConfig)
    config.serverType should ===(SERVER_TYPE_SOURCE)
    config.port should ===(8080)

    val topicKeys = Seq(TopicKey.of("default", "topic1"), TopicKey.of("default", "topic2"))

    config.sourceToTopics.size should ===(2)
    config.sourceToTopics(0) should ===(topicKeys(0))
    config.sourceToTopics(1) should ===(topicKeys(1))

    config.sinksFromTopics(0) should ===(topicKeys(0))
    config.sinksFromTopics(1) should ===(topicKeys(1))
  }

  @Test
  def testAllSettings(): Unit = {
    DefaultDefinitions.all(SERVER_TYPE_KEY) should ===(SERVER_TYPE_DEFINITION)
    DefaultDefinitions.all(CLIENT_PORT_KEY) should ===(CLIENT_PORT_DEFINITION)
    DefaultDefinitions.all(BROKERS_KEY) should ===(BROKERS_DEFINITION)
    DefaultDefinitions.all(SOURCE_TO_TOPICS_KEY) should ===(SOURCE_TO_TOPICS_DEFINITION)
    DefaultDefinitions.all(SINK_FROM_TOPICS_KEY) should ===(SINK_FROM_TOPICS_DEFINITION)
  }

  private def parseArgs(args: Array[String]): Map[String, String] =
    CommonUtils.parse(args.toSeq.asJava).asScala.toMap
}
