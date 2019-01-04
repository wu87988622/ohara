package com.island.ohara.agent
import com.island.ohara.client.ConfiguratorJson.Node

trait NodeCollie extends Iterable[Node] {
  def node(name: String): Node
}

object NodeCollie {

  def inMemory(nodes: Seq[Node]): NodeCollie = new NodeCollie {
    override def size: Int = nodes.size
    override def node(name: String): Node = nodes.find(_.name == name).get
    override def iterator: Iterator[Node] = nodes.iterator
  }
}
