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

package com.island.ohara.agent.ssh

import java.util

import com.island.ohara.agent.{ServiceCollie, DataCollie}
import com.island.ohara.client.configurator.v0.NodeApi.Node
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
import scala.util.{Failure, Success}

/**
  * the default implementation of verifying node consists of 4 actions.
  * 1) run "hello-world" on remote node
  * 2) check the existence of hello-world image
  * 3) check the status of hello-world container
  * 4) remove hello-world container
  * this test injects command handler for above actions that return correct response or throw exception.
  */
class TestVerifyNode extends OharaTest {
  private[this] var messageWhenFailToRun: String = _
  private[this] var messageWhenFailToListImages: String = _
  private[this] var messageWhenFailToPs: String = _
  private[this] var messageWhenFailToRemove: String = _
  private[this] val containerId = CommonUtils.randomString()
  private[this] var containerName: String = _
  private[this] val sshServer = SshdServer.local(
    0,
    Seq(
      new CommandHandler {
        override def belong(cmd: String): Boolean = cmd.contains("docker run -d") && cmd.contains("hello-world")
        override def execute(cmd: String): util.List[String] = if (messageWhenFailToRun != null)
          throw new IllegalArgumentException(messageWhenFailToRun)
        else {
          val splits = cmd.split(" ")
          val nameIndex = splits.zipWithIndex
            .filter {
              case (item, index) => item == "--name"
            }
            .head
            ._2
          containerName = splits(nameIndex + 1)
          util.Collections.singletonList(containerId)
        }
      },
      new CommandHandler {
        override def belong(cmd: String): Boolean = cmd.contains("docker images")
        override def execute(cmd: String): util.List[String] = if (messageWhenFailToListImages != null)
          throw new IllegalArgumentException(messageWhenFailToListImages)
        else util.Collections.singletonList("hello-world:latest")
      },
      new CommandHandler {
        override def belong(cmd: String): Boolean = cmd.contains("docker ps -a")
        override def execute(cmd: String): util.List[String] = if (messageWhenFailToPs != null)
          throw new IllegalArgumentException(messageWhenFailToPs)
        // the format used by DockerClientImpl is {{.ID}}\t{{.Names}}\t{{.Image}}
        else util.Collections.singletonList(s"${CommonUtils.randomString(10)}\t$containerName\tunknown/unknown:unknown")
      },
      new CommandHandler {
        override def belong(cmd: String): Boolean = cmd.contains("docker rm -f")
        override def execute(cmd: String): util.List[String] = if (messageWhenFailToRemove != null)
          throw new IllegalArgumentException(messageWhenFailToRemove)
        else util.Collections.singletonList(containerId)
      }
    ).asJava
  )

  private[this] val node = Node(
    hostname = sshServer.hostname(),
    port = Some(sshServer.port()),
    user = Some(sshServer.user()),
    password = Some(sshServer.password()),
    services = Seq.empty,
    lastModified = CommonUtils.current(),
    validationReport = None,
    resources = Seq.empty,
    tags = Map.empty
  )

  private[this] val collie = ServiceCollie.builderOfSsh.dataCollie(DataCollie(Seq(node))).build

  @Test
  def happyCase(): Unit = {
    Await.result(collie.verifyNode(node), 30 seconds) match {
      case Success(value)     => // pass
      case Failure(exception) => throw exception
    }
  }

  @Test
  def failToRunContainer(): Unit = {
    messageWhenFailToRun = CommonUtils.randomString()
    Await.result(collie.verifyNode(node), 30 seconds) match {
      case Success(value) => throw new AssertionError("this should fail!!!")
      case Failure(exception) =>
        exception.getMessage should include(messageWhenFailToRun)
    }
  }

  @Test
  def failToListImages(): Unit = {
    messageWhenFailToListImages = CommonUtils.randomString()
    Await.result(collie.verifyNode(node), 30 seconds) match {
      case Success(value) => throw new AssertionError("this should fail!!!")
      case Failure(exception) =>
        exception.getMessage should include(messageWhenFailToListImages)
    }
  }

  @Test
  def failToPs(): Unit = {
    messageWhenFailToPs = CommonUtils.randomString()
    Await.result(collie.verifyNode(node), 30 seconds) match {
      case Success(value) => throw new AssertionError("this should fail!!!")
      case Failure(exception) =>
        exception.getMessage should include(messageWhenFailToPs)
    }
  }

  @Test
  def failToRemove(): Unit = {
    messageWhenFailToRemove = CommonUtils.randomString()
    Await.result(collie.verifyNode(node), 30 seconds) match {
      case Success(value) => throw new AssertionError("this should fail!!!")
      case Failure(exception) =>
        exception.getMessage should include(messageWhenFailToRemove)
    }
  }

  @After
  def tearDown(): Unit = {
    Releasable.close(collie)
    Releasable.close(sshServer)
  }
}
