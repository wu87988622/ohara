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

import com.island.ohara.client.configurator.v0.NodeApi.NodeCreationRequest
import com.island.ohara.client.configurator.v0.{NodeApi, ZookeeperApi}
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterCreationRequest
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class TestClusterNameUpperCaseRoute extends SmallTest with Matchers {
  private[this] val numberOfCluster = 1
  private[this] val configurator =
    Configurator.builder().fake(numberOfCluster, numberOfCluster, "ZookeeperCluster").build()
  private[this] val nodeApi = NodeApi.access().hostname(configurator.hostname).port(configurator.port)
  private[this] val zookeeperApi = ZookeeperApi.access().hostname(configurator.hostname).port(configurator.port)
  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def testAddZookeeper(): Unit = {
    val req = NodeCreationRequest(Some("host1"), 22, "b", "c")
    result(nodeApi.add(req))

    an[IllegalArgumentException] should be thrownBy {
      val zkCreateRequest = ZookeeperClusterCreationRequest(
        name = CommonUtils.randomString(10),
        imageName = None,
        clientPort = Some(123),
        electionPort = Some(456),
        peerPort = Some(1345),
        nodeNames = Seq("host1")
      )
      result(zookeeperApi.add(zkCreateRequest))
    }
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
