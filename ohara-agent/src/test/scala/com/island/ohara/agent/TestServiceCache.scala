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

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.agent.ServiceCache.{RequestKey, Service}
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json.JsValue

import scala.concurrent.duration._
class TestServiceCache extends OharaTest with Matchers {

  @Test
  def testRequestKey(): Unit = {
    val key = RequestKey(
      key = ObjectKey.of(CommonUtils.randomString(), CommonUtils.randomString()),
      service = Service.WORKER,
      createdTime = CommonUtils.current()
    )

    key shouldBe key
    key should not be key.copy(key = ObjectKey.of(CommonUtils.randomString(), CommonUtils.randomString()))
    key should not be key.copy(service = Service.ZOOKEEPER)
  }

  @Test
  def testAutoRefresh(): Unit = {
    val clusterInfo0 = FakeClusterInfo(CommonUtils.randomString())
    val containerInfo0 = fakeContainerInfo()
    val clusterInfo1 = FakeClusterInfo(CommonUtils.randomString())
    val containerInfo1 = fakeContainerInfo()
    val fetchCount = new AtomicInteger(0)
    val refreshCount = new AtomicInteger(0)
    val cache = ServiceCache.builder
      .supplier(() => {
        refreshCount.incrementAndGet()
        Map(clusterInfo0 -> Seq(containerInfo0), clusterInfo1 -> Seq(containerInfo1))
      })
      .frequency(2 seconds)
      .build
    try {
      refreshCount.get() shouldBe 0
      fetchCount.get() shouldBe 0
      TimeUnit.SECONDS.sleep(3)
      refreshCount.get() shouldBe 1
      cache.snapshot(clusterInfo0) shouldBe Seq(containerInfo0)
      cache.snapshot(clusterInfo1) shouldBe Seq(containerInfo1)
      fetchCount.get() shouldBe 0
    } finally cache.close()
  }

  @Test
  def testRequestUpdate(): Unit = {
    val clusterInfo0 = FakeClusterInfo(CommonUtils.randomString())
    val containerInfo0 = fakeContainerInfo()
    val clusterInfo1 = FakeClusterInfo(CommonUtils.randomString())
    val containerInfo1 = fakeContainerInfo()
    val fetchCount = new AtomicInteger(0)
    val refreshCount = new AtomicInteger(0)
    val cache = ServiceCache.builder
      .supplier(() => {
        refreshCount.incrementAndGet()
        Map(clusterInfo0 -> Seq(containerInfo0), clusterInfo1 -> Seq(containerInfo1))
      })
      .frequency(1000 seconds)
      .build
    try {
      refreshCount.get() shouldBe 0
      cache.requestUpdate()
      TimeUnit.SECONDS.sleep(3)
      refreshCount.get() shouldBe 1
      cache.snapshot(clusterInfo0) shouldBe Seq(containerInfo0)
      cache.snapshot(clusterInfo1) shouldBe Seq(containerInfo1)
      fetchCount.get() shouldBe 0
      refreshCount.get() shouldBe 1
    } finally cache.close()
  }

  @Test
  def failToOperateAfterClose(): Unit = {
    val cache = ServiceCache.builder.supplier(() => Map.empty).frequency(1000 seconds).build
    cache.close()

    an[IllegalStateException] should be thrownBy cache.snapshot
    an[IllegalStateException] should be thrownBy cache.requestUpdate()
  }

  @Test
  def testGet(): Unit = {
    val clusterInfo0 = FakeClusterInfo(CommonUtils.randomString())
    val containerInfo0 = fakeContainerInfo()
    val cache = ServiceCache.builder
      .supplier(() => {
        Map(clusterInfo0 -> Seq(containerInfo0))
      })
      .frequency(1000 seconds)
      .build
    try {
      cache.get(clusterInfo0) shouldBe Seq.empty
      cache.put(clusterInfo0, Seq(containerInfo0))
      cache.get(clusterInfo0) shouldBe Seq(containerInfo0)
      cache.remove(clusterInfo0)
      cache.get(clusterInfo0) shouldBe Seq.empty
    } finally cache.close()
  }

  @Test
  def testLazyRemove(): Unit = {
    val count = new AtomicInteger(0)
    val cache = ServiceCache.builder
      .supplier(() => {
        count.incrementAndGet()
        Map.empty
      })
      .frequency(1 seconds)
      .lazyRemove(5 seconds)
      .build
    try {
      val clusterInfo = FakeClusterInfo(CommonUtils.randomString())
      val containerInfo = fakeContainerInfo()
      cache.put(clusterInfo, Seq(containerInfo))
      TimeUnit.SECONDS.sleep(2)
      val currentCount = count.get()
      currentCount should not be 0
      cache.get(clusterInfo) shouldBe Seq(containerInfo)
      TimeUnit.SECONDS.sleep(5)
      count.get() should not be currentCount
      cache.get(clusterInfo) shouldBe Seq.empty
    } finally cache.close()
  }

  @Test
  def testUpdateClusterInfo(): Unit = {
    val count = new AtomicInteger(0)
    val firstClusterInfo = FakeClusterInfo(CommonUtils.randomString())
    val cachedClusterInfo = new ClusterInfo {
      override def name: String = firstClusterInfo.name
      override def imageName: String = "123"
      override def ports: Set[Int] = Set.empty
      override def nodeNames: Set[String] = Set.empty
      override def aliveNodes: Set[String] = Set.empty

      override def group: String = "fake_group"

      override def lastModified: Long = CommonUtils.current()

      override def kind: String = "fake_cluster"

      override def tags: Map[String, JsValue] = Map.empty

      override def state: Option[String] = None

      override def error: Option[String] = None

      override def settings: Map[String, JsValue] = throw new UnsupportedOperationException
    }
    val cache = ServiceCache.builder
      .supplier(() => {
        count.incrementAndGet()
        Map(cachedClusterInfo -> Seq.empty)
      })
      .frequency(2 seconds)
      .lazyRemove(2 seconds)
      .build
    try {
      cache.snapshot.size shouldBe 0
      cache.put(firstClusterInfo, Seq.empty)
      cache.snapshot.size shouldBe 1
      TimeUnit.SECONDS.sleep(3)
      cache.snapshot.size shouldBe 1
      cache.get(cachedClusterInfo) shouldBe Seq.empty
      cache.snapshot.head._1 shouldBe cachedClusterInfo
    } finally cache.close()
  }

  private[this] def fakeContainerInfo(): ContainerInfo = ContainerInfo(
    nodeName = CommonUtils.randomString(),
    id = CommonUtils.randomString(),
    imageName = CommonUtils.randomString(),
    created = CommonUtils.randomString(),
    state = CommonUtils.randomString(),
    kind = CommonUtils.randomString(),
    name = CommonUtils.randomString(),
    size = CommonUtils.randomString(),
    portMappings = Seq.empty,
    environments = Map.empty,
    hostname = CommonUtils.randomString()
  )
}
