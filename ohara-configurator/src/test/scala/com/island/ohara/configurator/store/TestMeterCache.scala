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

package com.island.ohara.configurator.store

import java.util.concurrent.TimeUnit

import com.island.ohara.client.configurator.v0.MetricsApi.Meter
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.store.MeterCache.RequestKey
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._
class TestMeterCache extends OharaTest with Matchers {

  @Test
  def testRequestKey(): Unit = {
    val key = RequestKey(
      name = CommonUtils.randomString(),
      service = CommonUtils.randomString()
    )

    key shouldBe key
    key should not be key.copy(name = CommonUtils.randomString())
    key should not be key.copy(service = CommonUtils.randomString())
  }

  @Test
  def nullRefresher(): Unit =
    an[NullPointerException] should be thrownBy MeterCache.builder.refresher(null)

  @Test
  def nullFrequency(): Unit =
    an[NullPointerException] should be thrownBy MeterCache.builder.frequency(null)

  @Test
  def testRefresh(): Unit = {
    val data = Map(
      "name" -> Seq(
        Meter(
          value = 1.1,
          unit = "unit",
          document = "document",
          queryTime = CommonUtils.current(),
          startTime = Some(CommonUtils.current())
        )
      )
    )
    val clusterInfo = FakeClusterInfo(CommonUtils.randomString())
    val cache = MeterCache.builder.refresher(() => Map(clusterInfo -> data)).frequency(2 seconds).build
    try {
      cache.meters(clusterInfo) shouldBe Map.empty
      TimeUnit.SECONDS.sleep(3)
      cache.meters(clusterInfo) shouldBe data
    } finally cache.close()
  }

  @Test
  def failToOperateAfterClose(): Unit = {
    val cache = MeterCache.builder.refresher(() => Map.empty).frequency(2 seconds).build
    cache.close()

    an[IllegalStateException] should be thrownBy cache.meters(FakeClusterInfo(CommonUtils.randomString()))
  }
}
