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

import com.island.ohara.client.WorkerClient
import com.island.ohara.client.configurator.v0.ValidationApi.{
  FtpValidationRequest,
  HdfsValidationRequest,
  RdbValidationRequest,
  ValidationReport
}
import com.island.ohara.common.util.Releasable
import com.island.ohara.integration.With3Brokers3Workers
import com.island.ohara.kafka.BrokerClient
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestValidator extends With3Brokers3Workers with Matchers {
  private[this] val taskCount = 3
  private[this] val brokerClient = BrokerClient.of(testUtil.brokersConnProps)
  private[this] val ftpServer = testUtil.ftpServer
  private[this] val rdb = testUtil.dataBase
  private[this] val workerClient = WorkerClient(testUtil.workersConnProps)

  @Before
  def setup(): Unit = {
    workerClient.plugins().exists(_.className == classOf[Validator].getName) shouldBe true
  }

  private[this] def evaluate(f: Future[Seq[ValidationReport]]): Unit = {
    val reports = Await.result(f, 60 seconds)
    reports.isEmpty shouldBe false
    reports.foreach(_.pass shouldBe true)
  }

  @Test
  def testValidationOfHdfs(): Unit = {
    evaluate(Validator.run(workerClient, brokerClient, HdfsValidationRequest("file:///tmp"), taskCount))
  }

  @Test
  def testValidationOfFtp(): Unit = {
    evaluate(
      Validator.run(
        workerClient,
        brokerClient,
        FtpValidationRequest(ftpServer.hostname, ftpServer.port, ftpServer.user, ftpServer.password),
        taskCount
      ))
  }

  @Test
  def testValidationOfRdb(): Unit = {
    evaluate(
      Validator.run(
        workerClient,
        brokerClient,
        RdbValidationRequest(rdb.url, rdb.user, rdb.password),
        taskCount
      ))
  }

  @After
  def tearDown(): Unit = {
    Releasable.close(workerClient)
    Releasable.close(brokerClient)
  }
}
