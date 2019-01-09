package com.island.ohara.configurator.route

import com.island.ohara.client.configurator.v0.NodeApi
import com.island.ohara.client.configurator.v0.NodeApi.{Node, NodeCreationRequest}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.ReleaseOnce
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.{Await, Future}

class TestNodesRoute extends SmallTest with Matchers {

  private[this] val configurator = Configurator.fake()
  private[this] val access = NodeApi.access().hostname(configurator.hostname).port(configurator.port)

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
    lhs.lastModified shouldBe rhs.lastModified
  }
  @Test
  def testAdd(): Unit = {
    val req = NodeCreationRequest(Some("a"), 22, "b", "c")
    val res = result(access.add(req))
    compare(req, res)

    result(access.list()).size shouldBe 1
    compare(result(access.list()).head, res)
  }

  @Test
  def testDelete(): Unit = {
    val req = NodeCreationRequest(Some("a"), 22, "b", "c")
    val res = result(access.add(req))
    compare(req, res)

    result(access.list()).size shouldBe 1

    compare(result(access.delete(res.name)), res)
    result(access.list()).size shouldBe 0
  }

  @Test
  def testUpdate(): Unit = {
    val req = NodeCreationRequest(Some("a"), 22, "b", "c")
    val res = result(access.add(req))
    compare(req, res)

    result(access.list()).size shouldBe 1

    val req2 = NodeCreationRequest(Some("a"), 22, "b", "d")
    val res2 = result(access.update(res.name, req2))
    compare(req2, res2)

    result(access.list()).size shouldBe 1

    an[IllegalArgumentException] should be thrownBy result(
      access.update(res.id, NodeCreationRequest(Some("a2"), 22, "b", "d")))
  }

  @Test
  def testInvalidNameOfUpdate(): Unit = {
    val req = NodeCreationRequest(Some("a"), 22, "b", "c")
    val res = result(access.add(req))
    compare(req, res)

    result(access.list()).size shouldBe 1

    // we can't update an non-existent node
    an[IllegalArgumentException] should be thrownBy result(
      access.update("xxxxxx", NodeCreationRequest(Some("a"), 22, "b", "d")))
    // we can't update an existent node by unmatched name
    an[IllegalArgumentException] should be thrownBy result(
      access.update(res.id, NodeCreationRequest(Some("xxxxxx"), 22, "b", "d")))

    val req2 = NodeCreationRequest(Some(res.id), 22, "b", "d")
    val res2 = result(access.update(res.id, req2))
    compare(req2, res2)

    result(access.list()).size shouldBe 1

    val req3 = NodeCreationRequest(None, 22, "b", "zz")
    val res3 = result(access.update(res.id, req3))
    compare(req3, res3)
    result(access.list()).size shouldBe 1
  }

  @After
  def tearDown(): Unit = ReleaseOnce.close(configurator)
}
