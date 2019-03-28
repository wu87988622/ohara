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

package com.island.ohara.agent.wharf.stream

import com.island.ohara.agent.fake.FakeDockerClient
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerState
import com.island.ohara.client.configurator.v0.StreamApi
import com.island.ohara.common.rule.SmallTest
import org.junit.{Before, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class TestStreamCargo extends SmallTest with Matchers {

  private[this] val cargo: StreamCargo = StreamCargo(new FakeDockerClient)

  @Before
  def setup(): Unit = {}

  @Test
  def testNormalCasesWithFakeClient(): Unit = {

    val creator = cargo.createContainer()

    val info = Await.result(
      creator
        .imageName("streamApp")
        .jarUrl("jarUrl")
        .appId("myApp")
        .brokerList(Seq("broker1:9092", "broker2:9093"))
        .fromTopics(Seq("fromTopic1", "fromTopic2"))
        .toTopics(Seq("toTopic1", "toTopic2"))
        .create(),
      10 seconds
    )

    info.name shouldBe cargo.name
    info.hostname shouldBe cargo.name
    info.imageName shouldBe "streamApp"
    Seq(StreamApi.JARURL_KEY,
        StreamApi.APPID_KEY,
        StreamApi.SERVERS_KEY,
        StreamApi.FROM_TOPIC_KEY,
        StreamApi.TO_TOPIC_KEY).foreach { key =>
      info.environments.keys.toSeq.contains(key) shouldBe true
    }
    info.state shouldBe ContainerState.RUNNING.toString

    //another cargo creator
    val another = StreamCargo(new FakeDockerClient).createContainer()
    val info1 = Await.result(
      another
        .imageName("streamApp")
        .jarUrl("jarUrl")
        .appId("myApp")
        .brokerList(Seq("broker1:9092", "broker2:9093"))
        .fromTopics(Seq("fromTopic1", "fromTopic2"))
        .toTopics(Seq("toTopic1", "toTopic2"))
        .create(),
      10 seconds
    )

    // two different cargo should have different name
    info.name != info1.name shouldBe true
  }

  @Test
  def testMissingArguments(): Unit = {
    an[NullPointerException] should be thrownBy StreamCargo(null).createContainer().create()

    val creator = cargo.createContainer()

    // must specify arguments
    an[NullPointerException] should be thrownBy creator.create()

    an[IllegalArgumentException] should be thrownBy creator.imageName("").create()
    an[NullPointerException] should be thrownBy creator.imageName(null).create()

    an[IllegalArgumentException] should be thrownBy creator.jarUrl("").create()
    an[NullPointerException] should be thrownBy creator.jarUrl(null).create()

    an[IllegalArgumentException] should be thrownBy creator.appId("").create()
    an[NullPointerException] should be thrownBy creator.appId(null).create()

    an[NullPointerException] should be thrownBy creator.brokerList(Seq.empty).create()
    an[NullPointerException] should be thrownBy creator.brokerList(null).create()

    an[NullPointerException] should be thrownBy creator.fromTopics(Seq.empty).create()
    an[NullPointerException] should be thrownBy creator.fromTopics(null).create()

    an[NullPointerException] should be thrownBy creator.toTopics(Seq.empty).create()
    an[NullPointerException] should be thrownBy creator.toTopics(null).create()
  }
}
