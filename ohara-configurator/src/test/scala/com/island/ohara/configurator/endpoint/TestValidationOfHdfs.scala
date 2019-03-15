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

package com.island.ohara.configurator.endpoint

import com.island.ohara.client.configurator.v0.ValidationApi.{HdfsValidationRequest, ValidationReport}
import com.island.ohara.client.kafka.{TopicAdmin, WorkerClient}
import com.island.ohara.common.util.Releasable
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
class TestValidationOfHdfs extends With3Brokers3Workers with Matchers {
  private[this] val taskCount = 3
  private[this] val topicAdmin = TopicAdmin(testUtil.brokersConnProps)
  private[this] val workerClient = WorkerClient(testUtil.workersConnProps)

  @Before
  def setup(): Unit =
    Await.result(workerClient.plugins, 10 seconds).exists(_.className == classOf[Validator].getName) shouldBe true

  private[this] def result[T](f: Future[T]): T = Await.result(f, 60 seconds)

  private[this] def assertSuccess(f: Future[Seq[ValidationReport]]): Unit = {
    val reports = result(f)
    reports.size shouldBe taskCount
    reports.isEmpty shouldBe false
    reports.foreach(_.pass shouldBe true)
  }

  @Test
  def goodCase(): Unit = {
    assertSuccess(
      Validator
        .run(workerClient, topicAdmin, HdfsValidationRequest(uri = "file:///tmp", workerClusterName = None), taskCount))
  }

  @After
  def tearDown(): Unit = Releasable.close(topicAdmin)
}
