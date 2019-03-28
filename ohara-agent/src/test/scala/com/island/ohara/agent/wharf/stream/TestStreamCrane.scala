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
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.rule.SmallTest
import org.junit.{Before, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class TestStreamCrane extends SmallTest with Matchers {

  private[this] val SIZE: Int = 10
  private[this] val APP_ID = "FAKE_APP_ID"

  private[this] val FAKE_IMAGE = "fakeImage"

  private[this] val nodeCache: Seq[Node] = (1 to SIZE).map { i =>
    Node(
      name = s"fake_$i",
      port = 22,
      user = "fake",
      password = "fake"
    )
  }
  private[this] var crane: StreamCrane = _

  @Before
  def setup(): Unit = {
    val props = StreamCraneProps(
      imageName = FAKE_IMAGE,
      jarUrl = "fakeUrl",
      appId = APP_ID,
      brokerList = Seq("broker1:9092", "broker2:9093"),
      fromTopics = Seq("topic1", "topic2"),
      toTopics = Seq("topic3", "topic4")
    )

    crane = StreamCrane.builder().nodes(nodeCache).props(props).build()
  }

  @Test
  def testNormalCasesWithFakeClient(): Unit = {
    val fake = crane.fake()

    val res = Await.result(fake.add(2), 10 seconds)

    Await.result(fake.list, 10 seconds).size shouldBe 1

    Await.result(fake.get(res._1), 10 seconds).name shouldBe APP_ID

    Await.result(fake.delete(res._1), 10 seconds)

    Await.result(fake.list, 10 seconds).size shouldBe 0

    // streamCrane hierarchy verify
    val fakeCrane = Await.result(fake.add(2), 10 seconds)
    val warehouse = fakeCrane._2

    // we change the random string to unique appId
    warehouse.name shouldBe APP_ID

    val cargoes = Await.result(warehouse.list(), 10 seconds)
    cargoes.foreach { cargo =>
      val c = Await.result(cargo.info(), 10 seconds)
      c.imageName shouldBe FAKE_IMAGE
      c.name.startsWith("cargo-") shouldBe true
    }
  }

  @Test
  def testAddSameNameWarehouseHasNoEffect(): Unit = {
    val fake = crane.fake()

    // and same props but different (will get previous warehouse result)
    Await.result(fake.add(8), 10 seconds)
    // add is not impact, hence still 1
    Await.result(fake.list, 10 seconds).size shouldBe 1
  }

  @Test
  def testIllegalSize(): Unit = {
    an[RuntimeException] should be thrownBy Await.result(crane.add(SIZE + 1), 10 seconds)

    an[RuntimeException] should be thrownBy Await.result(crane.add(0), 10 seconds)

    an[RuntimeException] should be thrownBy Await.result(crane.add(-999), 10 seconds)
  }

  @Test
  def testIllegalArgument(): Unit = {
    an[NullPointerException] should be thrownBy StreamCrane.builder().nodes(null)
    an[NullPointerException] should be thrownBy StreamCrane.builder().props(null)
  }
}
