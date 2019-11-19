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

package com.island.ohara.configurator
import com.island.ohara.client.configurator.v0.ValidationApi
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.Releasable
import com.island.ohara.testing.service.SshdServer
import org.junit.{After, Test}
import org.scalatest.Matchers._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
class TestNodeValidation extends OharaTest {
  private[this] val configurator = Configurator.builder.fake().build()
  private[this] val sshd         = SshdServer.local()

  @Test
  def test(): Unit = {
    val reports = Await.result(
      ValidationApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .nodeRequest
        .hostname(sshd.hostname())
        .port(sshd.port())
        .user(sshd.user())
        .password(sshd.password())
        .verify(),
      10 seconds
    )
    reports.isEmpty shouldBe false
    reports.foreach(report => withClue(report.message)(report.pass shouldBe true))
  }

  @After
  def tearDown(): Unit = {
    Releasable.close(configurator)
    Releasable.close(sshd)
  }
}
