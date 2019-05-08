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

import com.island.ohara.agent.ClusterCache.{RequestKey, Service}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._
class TestClusterCache extends SmallTest with Matchers {

  @Test
  def testRequestKey(): Unit = {
    val key = RequestKey(
      name = CommonUtils.randomString(),
      service = Service.WORKER,
      clusterInfo = FakeClusterInfo(CommonUtils.randomString())
    )

    key shouldBe key
    key should not be key.copy(name = CommonUtils.randomString())
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
    val cache = ClusterCache
      .builder()
      .supplier(() => {
        refreshCount.incrementAndGet()
        Map(clusterInfo0 -> Seq(containerInfo0), clusterInfo1 -> Seq(containerInfo1))
      })
      .frequency(2 seconds)
      .build()
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
    val cache = ClusterCache
      .builder()
      .supplier(() => {
        refreshCount.incrementAndGet()
        Map(clusterInfo0 -> Seq(containerInfo0), clusterInfo1 -> Seq(containerInfo1))
      })
      .frequency(1000 seconds)
      .build()
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
    val cache = ClusterCache.builder().supplier(() => Map.empty).frequency(1000 seconds).build()
    cache.close()

    an[IllegalStateException] should be thrownBy cache.snapshot
    an[IllegalStateException] should be thrownBy cache.requestUpdate()
  }

  @Test
  def testGet(): Unit = {
    val clusterInfo0 = FakeClusterInfo(CommonUtils.randomString())
    val containerInfo0 = fakeContainerInfo()
    val cache = ClusterCache
      .builder()
      .supplier(() => {
        Map(clusterInfo0 -> Seq(containerInfo0))
      })
      .frequency(1000 seconds)
      .build()
    try {
      cache.get(clusterInfo0) shouldBe Seq.empty
      cache.put(clusterInfo0, Seq(containerInfo0))
      cache.get(clusterInfo0) shouldBe Seq(containerInfo0)
      cache.remove(clusterInfo0)
      cache.get(clusterInfo0) shouldBe Seq.empty
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
