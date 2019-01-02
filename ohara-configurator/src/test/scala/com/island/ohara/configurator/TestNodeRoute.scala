package com.island.ohara.configurator
import com.island.ohara.client.ConfiguratorClient
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.ReleaseOnce
import org.junit.{After, Test}
import org.scalatest.Matchers

class TestNodeRoute extends SmallTest with Matchers {

  private[this] val configurator = Configurator.local()
  private[this] val client = ConfiguratorClient(configurator.connectionProps)

  private[this] def compare(req: NodeRequest, res: Node): Unit = {
    req.name.map { name =>
      name shouldBe res.id
      name shouldBe res.name
    }
    req.port shouldBe res.port
    req.user shouldBe res.user
    req.password shouldBe res.password
  }

  @Test
  def testAdd(): Unit = {
    val req = NodeRequest(Some("a"), 22, "b", "c")
    val res = client.add[NodeRequest, Node](req)
    compare(req, res)

    client.list[Node].size shouldBe 1
    client.list[Node].head shouldBe res
  }

  @Test
  def testDelete(): Unit = {
    val req = NodeRequest(Some("a"), 22, "b", "c")
    val res = client.add[NodeRequest, Node](req)
    compare(req, res)

    client.list[Node].size shouldBe 1

    client.delete[Node](res.name) shouldBe res
    client.list[Node].size shouldBe 0
  }

  @Test
  def testUpdate(): Unit = {
    val req = NodeRequest(Some("a"), 22, "b", "c")
    val res = client.add[NodeRequest, Node](req)
    compare(req, res)

    client.list[Node].size shouldBe 1

    val req2 = NodeRequest(Some("a"), 22, "b", "d")
    val res2 = client.update[NodeRequest, Node](res.id, req2)
    compare(req2, res2)
    client.list[Node].size shouldBe 1

    an[IllegalArgumentException] should be thrownBy client
      .update[NodeRequest, Node](res.id, NodeRequest(Some("a2"), 22, "b", "d"))
  }

  @Test
  def testInvalidNameOfUpdate(): Unit = {
    val req = NodeRequest(Some("a"), 22, "b", "c")
    val res = client.add[NodeRequest, Node](req)
    compare(req, res)

    client.list[Node].size shouldBe 1

    // we can't update an non-existent node
    an[IllegalArgumentException] should be thrownBy client
      .update[NodeRequest, Node]("xxxxxx", NodeRequest(Some("a"), 22, "b", "d"))
    // we can't update an existent node by unmatched name
    an[IllegalArgumentException] should be thrownBy client
      .update[NodeRequest, Node](res.id, NodeRequest(Some("xxxxxx"), 22, "b", "d"))

    val req2 = NodeRequest(Some(res.id), 22, "b", "d")
    val res2 = client.update[NodeRequest, Node](res.id, req2)
    compare(req2, res2)
    client.list[Node].size shouldBe 1

    val req3 = NodeRequest(None, 22, "b", "zz")
    val res3 = client.update[NodeRequest, Node](res.id, req3)
    compare(req3, res3)
    client.list[Node].size shouldBe 1
  }

  @After
  def tearDown(): Unit = {
    ReleaseOnce.close(client)
    ReleaseOnce.close(configurator)
  }
}
