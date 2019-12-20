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

package com.island.ohara.agent.docker

import java.util

import com.island.ohara.agent.{DataCollie, ServiceCollie}
import com.island.ohara.client.configurator.v0.NodeApi.{Node, State}
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.testing.service.SshdServer
import com.island.ohara.testing.service.SshdServer.CommandHandler
import org.junit.{After, Test}
import org.scalatest.Matchers._

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * the default implementation of verifying node consists of 1 actions.
  * 1) list resources
  * this test injects command handler for above actions that return correct response or throw exception.
  */
class TestVerifyNode extends OharaTest {
  private[this] var errorMessage: String = _
  private[this] val sshServer = SshdServer.local(
    0,
    Seq(
      /**
        * this commend is used by ServiceCollieImpl#verifyNode.
        */
      new CommandHandler {
        override def belong(cmd: String): Boolean = cmd.contains("docker info --format '{{json .}}'")
        override def execute(cmd: String): util.List[String] =
          if (errorMessage != null)
            throw new IllegalArgumentException(errorMessage)
          else util.Collections.singletonList("""
              |  {
              |    "NCPU": 1,
              |    "MemTotal": 1024
              |  }
              |""".stripMargin)
      }
    ).asJava
  )

  private[this] val node = Node(
    hostname = sshServer.hostname(),
    port = Some(sshServer.port()),
    user = Some(sshServer.user()),
    password = Some(sshServer.password()),
    services = Seq.empty,
    state = State.AVAILABLE,
    error = None,
    lastModified = CommonUtils.current(),
    resources = Seq.empty,
    tags = Map.empty
  )

  private[this] val collie = ServiceCollie.dockerModeBuilder.dataCollie(DataCollie(Seq(node))).build

  @Test
  def happyCase(): Unit = Await.result(collie.verifyNode(node), 30 seconds)

  @Test
  def badCase(): Unit = {
    errorMessage = CommonUtils.randomString()
    intercept[Exception] {
      Await.result(collie.verifyNode(node), 30 seconds)
    }.getMessage should include("unavailable")
  }

  @After
  def tearDown(): Unit = {
    Releasable.close(collie)
    Releasable.close(sshServer)
  }
}
