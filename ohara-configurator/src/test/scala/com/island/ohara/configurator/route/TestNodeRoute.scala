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

import com.island.ohara.client.configurator.v0.NodeApi
import com.island.ohara.client.configurator.v0.NodeApi.{Node, NodeCreationRequest}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.Releasable
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.{Await, Future}

class TestNodeRoute extends SmallTest with Matchers {
  private[this] val numberOfCluster = 1
  private[this] val configurator = Configurator.builder().fake(numberOfCluster, numberOfCluster).build()

  /**
    * a fake cluster has 3 fake node.
    */
  private[this] val numberOfDefaultNodes = 3 * numberOfCluster
  private[this] val nodeApi = NodeApi.access().hostname(configurator.hostname).port(configurator.port)

  import scala.concurrent.duration._
  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)
  private[this] def compare(req: NodeCreationRequest, res: Node): Unit = {
    req.name.map { name =>
      name shouldBe res.id
      name shouldBe res.name
    }
    req.port shouldBe res.port
    req.user shouldBe res.user
    req.password shouldBe res.password
    res.services.isEmpty shouldBe false
    res.services.foreach(_.clusterNames.isEmpty shouldBe true)
  }

  private[this] def compare(lhs: Node, rhs: Node): Unit = {
    lhs.name shouldBe rhs.name
    lhs.port shouldBe rhs.port
    lhs.user shouldBe rhs.user
    lhs.password shouldBe rhs.password
  }

  @Test
  def testServices(): Unit = {
    val nodes = result(nodeApi.list())
    nodes.isEmpty shouldBe false
    nodes.foreach(_.services.isEmpty shouldBe false)
  }

  @Test
  def testAdd(): Unit = {
    val req = NodeCreationRequest(Some("a"), 22, "b", "c")
    val res = result(nodeApi.add(req))
    compare(req, res)

    result(nodeApi.list()).size shouldBe (1 + numberOfDefaultNodes)
    compare(result(nodeApi.list()).find(_.name == req.name.get).get, res)
  }

  @Test
  def testDelete(): Unit = {
    val req = NodeCreationRequest(Some("a"), 22, "b", "c")
    val res = result(nodeApi.add(req))
    compare(req, res)

    result(nodeApi.list()).size shouldBe (1 + numberOfDefaultNodes)

    compare(result(nodeApi.delete(res.name)), res)
    result(nodeApi.list()).size shouldBe numberOfDefaultNodes
  }

  @Test
  def disableToDeleteNodeRunningService(): Unit = {
    val nodes = result(nodeApi.list())
    val runningNode = nodes.filter(_.services.exists(_.clusterNames.nonEmpty)).head

    an[IllegalArgumentException] should be thrownBy result(nodeApi.delete(runningNode.id))
  }

  @Test
  def testUpdate(): Unit = {
    val req = NodeCreationRequest(Some("a"), 22, "b", "c")
    val res = result(nodeApi.add(req))
    compare(req, res)

    result(nodeApi.list()).size shouldBe (1 + numberOfDefaultNodes)

    val req2 = NodeCreationRequest(Some("a"), 22, "b", "d")
    val res2 = result(nodeApi.update(res.name, req2))
    compare(req2, res2)

    result(nodeApi.list()).size shouldBe (1 + numberOfDefaultNodes)

    an[IllegalArgumentException] should be thrownBy result(
      nodeApi.update(res.id, NodeCreationRequest(Some("a2"), 22, "b", "d")))
  }

  @Test
  def testInvalidNameOfUpdate(): Unit = {
    val req = NodeCreationRequest(Some("a"), 22, "b", "c")
    val res = result(nodeApi.add(req))
    compare(req, res)

    result(nodeApi.list()).size shouldBe (1 + numberOfDefaultNodes)

    // we can't update an non-existent node
    an[IllegalArgumentException] should be thrownBy result(
      nodeApi.update("xxxxxx", NodeCreationRequest(Some("a"), 22, "b", "d")))
    // we can't update an existent node by unmatched name
    an[IllegalArgumentException] should be thrownBy result(
      nodeApi.update(res.id, NodeCreationRequest(Some("xxxxxx"), 22, "b", "d")))

    val req2 = NodeCreationRequest(Some(res.id), 22, "b", "d")
    val res2 = result(nodeApi.update(res.id, req2))
    compare(req2, res2)

    result(nodeApi.list()).size shouldBe (1 + numberOfDefaultNodes)

    val req3 = NodeCreationRequest(None, 22, "b", "zz")
    val res3 = result(nodeApi.update(res.id, req3))
    compare(req3, res3)
    result(nodeApi.list()).size shouldBe (1 + numberOfDefaultNodes)
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
