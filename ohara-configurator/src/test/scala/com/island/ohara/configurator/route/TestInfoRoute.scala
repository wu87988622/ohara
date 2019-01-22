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

package com.island.ohara.configurator.route

import com.island.ohara.client.configurator.v0.InfoApi
import com.island.ohara.common.data.DataType
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{Releasable, VersionUtil}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestInfoRoute extends SmallTest with Matchers {
  private[this] val configurator = Configurator.builder().fake().build()

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def test(): Unit = {
    // only test the configurator based on mini cluster
    val clusterInformation = result(InfoApi.access().hostname(configurator.hostname).port(configurator.port).get())
    clusterInformation.brokers shouldBe "this field is deprecated. Use Brokers APIs instead"
    clusterInformation.workers shouldBe "this field is deprecated. Use Workers APIs instead"
    clusterInformation.supportedDatabases.contains("mysql") shouldBe true
    clusterInformation.supportedDataTypes shouldBe DataType.all.asScala
    clusterInformation.sources.size shouldBe 0
    clusterInformation.sinks.size shouldBe 0
    clusterInformation.versionInfo.version shouldBe VersionUtil.VERSION
    clusterInformation.versionInfo.user shouldBe VersionUtil.USER
    clusterInformation.versionInfo.revision shouldBe VersionUtil.REVISION
    clusterInformation.versionInfo.date shouldBe VersionUtil.DATE
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
