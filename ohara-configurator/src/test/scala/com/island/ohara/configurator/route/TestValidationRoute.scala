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

import com.island.ohara.client.configurator.v0.{ValidationApi, WorkerApi}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.{Configurator, DumbSink}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
class TestValidationRoute extends SmallTest with Matchers {
  private[this] val configurator = Configurator.builder().fake().build()

  private[this] val wkCluster = result(WorkerApi.access().hostname(configurator.hostname).port(configurator.port).list).head

  @Test
  def validateConnector(): Unit = {
    val className = classOf[DumbSink].getName
    val response = result(
      ValidationApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .connectorRequest
        .name(CommonUtils.randomString(10))
        .className(className)
        .topicName(CommonUtils.randomString(10))
        .workerClusterName(wkCluster.name)
        .verify()
    )
    response.className.get() shouldBe className
  }

  @Test
  def validateHdfs(): Unit = {
    val report = result(
      ValidationApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .hdfsRequest
        .uri("file:///tmp")
        .verify())
    report.isEmpty shouldBe false
    report.foreach(_.pass shouldBe true)
  }

  @Test
  def validateHdfsOnNonexistentWorkerCluster(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      ValidationApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .hdfsRequest
        .uri("file:///tmp")
        .workerClusterName(CommonUtils.randomString(10))
        .verify())
  }

  @Test
  def validateRdb(): Unit = {
    val report = result(
      ValidationApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .rdbRequest
        .url("fake_url")
        .user("fake_user")
        .password("fake_password")
        .verify())
    report.isEmpty shouldBe false
    report.foreach(_.pass shouldBe true)
    report.foreach(_.rdbInfo.tables.isEmpty shouldBe false)
  }

  @Test
  def validateRbdOnNonexistentWorkerCluster(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      ValidationApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .rdbRequest
        .url("fake_url")
        .user("fake_user")
        .password("fake_password")
        .workerClusterName(CommonUtils.randomString(10))
        .verify())
  }

  @Test
  def validateFtp(): Unit = {
    val report = result(
      ValidationApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .ftpRequest
        .hostname("fake_server")
        .port(22)
        .user("fake_user")
        .password("fake_password")
        .verify())
    report.isEmpty shouldBe false
    report.foreach(_.pass shouldBe true)
  }

  @Test
  def validateFtpOnNonexistentWorkerCluster(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      ValidationApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .ftpRequest
        .hostname("fake_server")
        .port(22)
        .user("fake_user")
        .password("fake_password")
        .workerClusterName(CommonUtils.randomString(10))
        .verify())
  }

  @Test
  def validateNode(): Unit = {
    val report = result(
      ValidationApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .nodeRequest
        .hostname("fake_server")
        .port(22)
        .user("fake_user")
        .password("fake_password")
        .verify())
    report.isEmpty shouldBe false
    report.foreach(_.pass shouldBe true)
  }

  @Test
  def testFakeReport(): Unit = result(ValidationRoute.fakeReport()).foreach(_.pass shouldBe true)

  @Test
  def testFakeJdbcReport(): Unit = result(ValidationRoute.fakeJdbcReport()).foreach { report =>
    report.pass shouldBe true
    report.rdbInfo.tables.isEmpty shouldBe false
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
