package com.island.ohara.agent
import com.island.ohara.agent.AgentJson.Node
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestNodeCollie extends SmallTest with Matchers {

  @Test
  def testAdd(): Unit = {
    val collie = NodeCollie.inMemory()
    try {
      val node = Node("b", 123, "c", "d")
      collie.add(node)
      collie.size shouldBe 1
      an[IllegalArgumentException] should be thrownBy collie.add(node)
      collie.size shouldBe 1
      collie.head shouldBe node
    } finally collie.close()
  }

  @Test
  def testUpdate(): Unit = {
    val collie = NodeCollie.inMemory()
    try {
      val node = Node("b", 123, "c", "d")
      collie.add(node)
      collie.size shouldBe 1
      val node2 = Node("b", 123, "c", "d")
      collie.update(node2)
      collie.size shouldBe 1
      collie.head shouldBe node2
      val node3 = Node("b3", 123, "c", "d")
      an[IllegalArgumentException] should be thrownBy collie.update(node3)
    } finally collie.close()
  }

  @Test
  def testRemove(): Unit = {
    val collie = NodeCollie.inMemory()
    try {
      val node = Node("b", 123, "c", "d")
      val node2 = Node("b2", 123, "c", "d")
      collie.add(node)
      collie.add(node2)
      collie.size shouldBe 2
      collie.remove(node.name)
      collie.size shouldBe 1
      collie.head shouldBe node2
    } finally collie.close()
  }
  @Test
  def testClose(): Unit = {
    val collie = NodeCollie.inMemory()
    try {
      val node = Node("b", 123, "c", "d")
      collie.add(node)
      collie.size shouldBe 1
    } finally collie.close()
    collie.size shouldBe 0
  }
}
