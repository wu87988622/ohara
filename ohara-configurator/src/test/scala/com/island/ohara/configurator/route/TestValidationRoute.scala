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

import com.island.ohara.client.configurator.v0.ValidationApi
import com.island.ohara.client.configurator.v0.ValidationApi.{
  FtpValidationRequest,
  HdfsValidationRequest,
  NodeValidationRequest,
  RdbValidationRequest
}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtil, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestValidationRoute extends SmallTest with Matchers {
  private[this] val configurator = Configurator.builder().fake().build()

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def validateHdfs(): Unit = {
    val report = result(
      ValidationApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .verify(HdfsValidationRequest(uri = "file:///tmp", workerClusterName = None)))
    report.isEmpty shouldBe false
    report.foreach(_.pass shouldBe true)
  }

  @Test
  def validateHdfsOnNonexistentWorkerCluster(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      ValidationApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .verify(HdfsValidationRequest(uri = "file:///tmp", workerClusterName = Some(CommonUtil.randomString(10)))))
  }

  @Test
  def validateRdb(): Unit = {
    val report = result(
      ValidationApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .verify(
          RdbValidationRequest(url = "fake_url",
                               user = "fake_user",
                               password = "fake_password",
                               workerClusterName = None)))
    report.isEmpty shouldBe false
    report.foreach(_.pass shouldBe true)
  }

  @Test
  def validateRbdOnNonexistentWorkerCluster(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      ValidationApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .verify(
          RdbValidationRequest(url = "fake_url",
                               user = "fake_user",
                               password = "fake_password",
                               workerClusterName = Some(CommonUtil.randomString(10)))))
  }

  @Test
  def validateFtp(): Unit = {
    val report = result(
      ValidationApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .verify(
          FtpValidationRequest(hostname = "fake_server",
                               port = 22,
                               user = "fake_user",
                               password = "fake_password",
                               workerClusterName = None)))
    report.isEmpty shouldBe false
    report.foreach(_.pass shouldBe true)
  }

  @Test
  def validateFtpOnNonexistentWorkerCluster(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      ValidationApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .verify(
          FtpValidationRequest(hostname = "fake_server",
                               port = 22,
                               user = "fake_user",
                               password = "fake_password",
                               workerClusterName = Some(CommonUtil.randomString(10)))))
  }

  @Test
  def validateNode(): Unit = {
    val report = result(
      ValidationApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .verify(NodeValidationRequest("fake_server", 22, "fake_user", "fake_password")))
    report.isEmpty shouldBe false
    report.foreach(_.pass shouldBe true)
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
