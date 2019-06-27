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

package com.island.ohara.configurator.validation

import com.island.ohara.client.configurator.v0.QueryApi.RdbColumn
import com.island.ohara.client.configurator.v0.ValidationApi.RdbValidation
import com.island.ohara.client.database.DatabaseClient
import com.island.ohara.client.kafka.{TopicAdmin, WorkerClient}
import com.island.ohara.common.util.Releasable
import com.island.ohara.configurator.route.ValidationUtils
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class TestValidationOfRdb extends With3Brokers3Workers with Matchers {
  private[this] val topicAdmin = TopicAdmin(testUtil.brokersConnProps)
  private[this] val rdb = testUtil.dataBase
  private[this] val workerClient = WorkerClient(testUtil.workersConnProps)

  @Before
  def setup(): Unit =
    Await
      .result(workerClient.plugins(), 10 seconds)
      .exists(_.className == "com.island.ohara.connector.validation.Validator") shouldBe true

  @Test
  def goodCase(): Unit = {
    val client = DatabaseClient.builder.url(rdb.url()).user(rdb.user()).password(rdb.password()).build
    try client.createTable("table", Seq(RdbColumn("v0", "integer", true)))
    finally client.close()
    assertJdbcSuccess(
      workerClient,
      ValidationUtils.run(
        workerClient,
        topicAdmin,
        RdbValidation(url = rdb.url, user = rdb.user, password = rdb.password, workerClusterName = None),
        NUMBER_OF_TASKS
      )
    )
  }
  @After
  def tearDown(): Unit = Releasable.close(topicAdmin)
}
