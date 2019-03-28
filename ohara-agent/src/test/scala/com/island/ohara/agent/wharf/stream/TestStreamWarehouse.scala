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

class TestStreamWarehouse extends SmallTest with Matchers {

  private[this] val SIZE: Int = 10

  private[this] val nodeCache: Seq[Node] = (1 to SIZE).map { i =>
    Node(
      name = s"fake_$i",
      port = 22,
      user = "fake_user",
      password = "fake_password"
    )
  }
  private[this] var fake: StreamWarehouse = _

  @Before
  def setup(): Unit = {
    fake = StreamWarehouse(
      nodes = nodeCache,
      warehouseName = "fake_warehouse",
      imageName = "fakeImage",
      jarUrl = "fakeUrl",
      appId = "appId",
      brokerList = Seq("broker1:9092", "broker2:9093"),
      fromTopics = Seq("topic1", "topic2"),
      toTopics = Seq("topic3", "topic4"),
      isFake = true
    )
  }

  @Test
  def testNormalCasesWithFakeClient(): Unit = {

    val res = Await.result(fake.add(nodeCache.map(_.name)), 10 seconds)

    res.size shouldBe 10

    Await.result(fake.get(res.head.name), 10 seconds).get shouldBe res.head

    Await.result(fake.delete(res.last.name), 10 seconds)

    Await.result(fake.list, 10 seconds).size shouldBe 9
  }

  @Test
  def testIllegalArguments(): Unit = {
    an[NullPointerException] should be thrownBy StreamWarehouse(
      nodes = nodeCache,
      warehouseName = null,
      imageName = "fakeImage",
      jarUrl = "fakeUrl",
      appId = "appId",
      brokerList = Seq("broker1:9092", "broker2:9093"),
      fromTopics = Seq("topic1", "topic2"),
      toTopics = Seq("topic3", "topic4"),
      isFake = true
    )

    an[IllegalArgumentException] should be thrownBy StreamWarehouse(
      nodes = nodeCache,
      warehouseName = "",
      imageName = "fakeImage",
      jarUrl = "fakeUrl",
      appId = "appId",
      brokerList = Seq("broker1:9092", "broker2:9093"),
      fromTopics = Seq("topic1", "topic2"),
      toTopics = Seq("topic3", "topic4"),
      isFake = true
    )

    an[NullPointerException] should be thrownBy StreamWarehouse(
      nodes = nodeCache,
      warehouseName = "fakeWarehouse",
      imageName = null,
      jarUrl = "fakeUrl",
      appId = "appId",
      brokerList = Seq("broker1:9092", "broker2:9093"),
      fromTopics = Seq("topic1", "topic2"),
      toTopics = Seq("topic3", "topic4"),
      isFake = true
    )

    an[IllegalArgumentException] should be thrownBy StreamWarehouse(
      nodes = nodeCache,
      warehouseName = "fakeWarehouse",
      imageName = "",
      jarUrl = "fakeUrl",
      appId = "appId",
      brokerList = Seq("broker1:9092", "broker2:9093"),
      fromTopics = Seq("topic1", "topic2"),
      toTopics = Seq("topic3", "topic4"),
      isFake = true
    )

    an[NullPointerException] should be thrownBy StreamWarehouse(
      nodes = nodeCache,
      warehouseName = "fakeWarehouse",
      imageName = "fakeImage",
      jarUrl = null,
      appId = "appId",
      brokerList = Seq("broker1:9092", "broker2:9093"),
      fromTopics = Seq("topic1", "topic2"),
      toTopics = Seq("topic3", "topic4"),
      isFake = true
    )

    an[IllegalArgumentException] should be thrownBy StreamWarehouse(
      nodes = nodeCache,
      warehouseName = "fakeWarehouse",
      imageName = "fakeImage",
      jarUrl = "",
      appId = "appId",
      brokerList = Seq("broker1:9092", "broker2:9093"),
      fromTopics = Seq("topic1", "topic2"),
      toTopics = Seq("topic3", "topic4"),
      isFake = true
    )

    an[NullPointerException] should be thrownBy StreamWarehouse(
      nodes = nodeCache,
      warehouseName = "fakeWarehouse",
      imageName = "fakeImage",
      jarUrl = "fakeUrl",
      appId = null,
      brokerList = Seq("broker1:9092", "broker2:9093"),
      fromTopics = Seq("topic1", "topic2"),
      toTopics = Seq("topic3", "topic4"),
      isFake = true
    )

    an[IllegalArgumentException] should be thrownBy StreamWarehouse(
      nodes = nodeCache,
      warehouseName = "fakeWarehouse",
      imageName = "fakeImage",
      jarUrl = "fakeUrl",
      appId = "",
      brokerList = Seq("broker1:9092", "broker2:9093"),
      fromTopics = Seq("topic1", "topic2"),
      toTopics = Seq("topic3", "topic4"),
      isFake = true
    )

    an[NullPointerException] should be thrownBy StreamWarehouse(
      nodes = nodeCache,
      warehouseName = "fakeWarehouse",
      imageName = "fakeImage",
      jarUrl = "fakeUrl",
      appId = "appId",
      brokerList = null,
      fromTopics = Seq("topic1", "topic2"),
      toTopics = Seq("topic3", "topic4"),
      isFake = true
    )

    an[NullPointerException] should be thrownBy StreamWarehouse(
      nodes = nodeCache,
      warehouseName = "fakeWarehouse",
      imageName = "fakeImage",
      jarUrl = "fakeUrl",
      appId = "appId",
      brokerList = Seq("broker1:9092", "broker2:9093"),
      fromTopics = null,
      toTopics = Seq("topic3", "topic4"),
      isFake = true
    )

    an[NullPointerException] should be thrownBy StreamWarehouse(
      nodes = nodeCache,
      warehouseName = "fakeWarehouse",
      imageName = "fakeImage",
      jarUrl = "fakeUrl",
      appId = "appId",
      brokerList = Seq("broker1:9092", "broker2:9093"),
      fromTopics = Seq("topic1", "topic2"),
      toTopics = null,
      isFake = true
    )
  }
}
