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

import com.island.ohara.client.configurator.v0.MetricsApi.Meter
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.store.MeterCache.RequestKey
import org.junit.Test
import org.scalatest.Matchers

class TestMeterCache extends SmallTest with Matchers {

  @Test
  def testRequestKey(): Unit = {
    val key = RequestKey(
      name = CommonUtils.randomString(),
      service = CommonUtils.randomString(),
      clusterInfo = FakeClusterInfo(CommonUtils.randomString())
    )

    key shouldBe key
    key should not be key.copy(name = CommonUtils.randomString())
    key should not be key.copy(service = CommonUtils.randomString())
  }

  @Test
  def nullRefresher(): Unit =
    an[NullPointerException] should be thrownBy MeterCache.builder().refresher(null)

  @Test
  def nullFetcher(): Unit =
    an[NullPointerException] should be thrownBy MeterCache.builder().fetcher(null)

  @Test
  def nullTimeout(): Unit =
    an[NullPointerException] should be thrownBy MeterCache.builder().timeout(null)

  @Test
  def nullFrequency(): Unit =
    an[NullPointerException] should be thrownBy MeterCache.builder().frequency(null)

  @Test
  def testFetcher(): Unit = {
    val data = Map(
      "name" -> Seq(
        Meter(
          value = 1.1,
          unit = "unit",
          document = "document"
        )
      )
    )
    val clusterInfo = FakeClusterInfo(CommonUtils.randomString())
    val cache = MeterCache.builder().fetcher(_ => data).refresher(() => Map(clusterInfo -> data)).build()
    cache.meters(clusterInfo) shouldBe data
  }
}
