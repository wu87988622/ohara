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

import com.island.ohara.client.configurator.v0.NodeApi
import com.island.ohara.client.configurator.v0.NodeApi.NodeCreationRequest
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.Releasable
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
class TestOhara1255 extends SmallTest with Matchers {

  private[this] val numberOfCluster = 1
  private[this] val configurator = Configurator.builder().fake(numberOfCluster, numberOfCluster).build() /**
    * a fake cluster has 3 fake node.
    */
  private[this] val numberOfDefaultNodes = 3 * numberOfCluster

  private[this] val access = NodeApi.access().hostname("localhost").port(configurator.port)

  @Test
  def testAddInvalidNodes(): Unit = {
    val numberOfRequest = 10
    (0 until numberOfRequest)
      .map(
        i =>
          NodeCreationRequest(
            name = Some(i.toString),
            port = i,
            user = i.toString,
            password = i.toString
        ))
      .foreach(r => Await.result(access.add(r), 10 seconds))

    Await.result(access.list(), 10 seconds).size shouldBe (numberOfRequest + numberOfDefaultNodes)
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
