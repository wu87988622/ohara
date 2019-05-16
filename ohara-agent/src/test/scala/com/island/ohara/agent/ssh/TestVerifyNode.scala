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

import com.island.ohara.agent.{ClusterCollie, NodeCollie}
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.testing.service.SshdServer
import com.island.ohara.testing.service.SshdServer.CommandHandler
import org.junit.{After, Test}
import org.scalatest.Matchers

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
class TestVerifyNode extends SmallTest with Matchers {
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
        else util.Collections.singletonList(containerName)
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
    name = sshServer.hostname(),
    port = sshServer.port(),
    user = sshServer.user(),
    password = sshServer.password()
  )

  private[this] val collie = ClusterCollie.builderOfSsh().nodeCollie(NodeCollie(Seq(node))).build()

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
        withClue(exception.getMessage)(exception.getMessage.contains(messageWhenFailToRun) shouldBe true)
    }
  }

  @Test
  def failToListImages(): Unit = {
    messageWhenFailToListImages = CommonUtils.randomString()
    Await.result(collie.verifyNode(node), 30 seconds) match {
      case Success(value) => throw new AssertionError("this should fail!!!")
      case Failure(exception) =>
        withClue(exception.getMessage)(exception.getMessage.contains(messageWhenFailToListImages) shouldBe true)
    }
  }

  @Test
  def failToPs(): Unit = {
    messageWhenFailToPs = CommonUtils.randomString()
    Await.result(collie.verifyNode(node), 30 seconds) match {
      case Success(value) => throw new AssertionError("this should fail!!!")
      case Failure(exception) =>
        withClue(exception.getMessage)(exception.getMessage.contains(messageWhenFailToPs) shouldBe true)
    }
  }

  @Test
  def failToRemove(): Unit = {
    messageWhenFailToRemove = CommonUtils.randomString()
    Await.result(collie.verifyNode(node), 30 seconds) match {
      case Success(value) => throw new AssertionError("this should fail!!!")
      case Failure(exception) =>
        withClue(exception.getMessage)(exception.getMessage.contains(messageWhenFailToRemove) shouldBe true)
    }
  }

  @After
  def tearDown(): Unit = {
    Releasable.close(collie)
    Releasable.close(sshServer)
  }
}
