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
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.{Releasable, VersionUtils}
import com.island.ohara.configurator.Configurator
import com.island.ohara.configurator.Configurator.Mode
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
class TestInfoRoute extends OharaTest with Matchers {
  private[this] val configurator = Configurator.builder.fake().build()

  private[this] def result[T](f: Future[T]): T = Await.result(f, Duration("20 seconds"))
  @Test
  def test(): Unit = {
    // only test the configurator based on mini cluster
    val clusterInformation = result(InfoApi.access.hostname(configurator.hostname).port(configurator.port).get)
    clusterInformation.versionInfo.version shouldBe VersionUtils.VERSION
    clusterInformation.versionInfo.branch shouldBe VersionUtils.BRANCH
    clusterInformation.versionInfo.user shouldBe VersionUtils.USER
    clusterInformation.versionInfo.revision shouldBe VersionUtils.REVISION
    clusterInformation.versionInfo.date shouldBe VersionUtils.DATE
    clusterInformation.mode shouldBe Mode.FAKE.toString
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
