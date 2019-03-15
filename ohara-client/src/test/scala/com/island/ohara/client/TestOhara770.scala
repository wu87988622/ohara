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

package com.island.ohara.client
import com.island.ohara.client.kafka.WorkerJson.ConnectorCreationResponse
import com.island.ohara.client.kafka.{WorkerClient, WorkerJson}
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
class TestOhara770 extends SmallTest with Matchers {

  @Test
  def configsNameShouldBeRemoved(): Unit = {
    class DumbConnectorCreator extends WorkerClient.Creator {
      override protected def doCreate(
        executionContext: ExecutionContext,
        request: WorkerJson.ConnectorCreationRequest): Future[WorkerJson.ConnectorCreationResponse] = Future {
        request.configs.get("name") shouldBe None
        ConnectorCreationResponse(
          name = "adas",
          config = Map.empty,
          tasks = Seq.empty
        )
      }
    }

    val creator = new DumbConnectorCreator()
    // this should pass
    Await
      .result(creator.name("abc").className("asdasd").topicName("aaa").configs(Map("name" -> "aa")).create, 10 seconds)
  }

}
