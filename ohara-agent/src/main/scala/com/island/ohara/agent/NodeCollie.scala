package com.island.ohara.agent
import java.util.concurrent.ConcurrentHashMap

import com.island.ohara.agent.AgentJson.Node
import com.island.ohara.common.util.{Releasable, ReleaseOnce}

trait NodeCollie extends Releasable with Iterable[Node] {
  def add(node: Node): Unit
  def remove(uuid: String): Unit
  def update(node: Node): Unit

  def get(name: String): Node = this.find(_.name == name).get
}

object NodeCollie {

  private[this] class NodeCollieImpl(nodes: Seq[Node]) extends ReleaseOnce with NodeCollie {
    private[this] val cache = {
      val c = new ConcurrentHashMap[String, Node]()
      nodes.foreach(n => c.put(n.name, n))
      c
    }
    override protected def doClose(): Unit = cache.clear()
    import scala.collection.JavaConverters._
    override def iterator: Iterator[Node] = cache.values().asScala.toIterator
    override def add(node: Node): Unit =
      if (cache.putIfAbsent(node.name, node) != null) throw new IllegalArgumentException(s"${node.name} exists")
    override def remove(uuid: String): Unit =
      if (cache.remove(uuid) == null) throw new IllegalArgumentException(s"$uuid doesn't exist")
    override def update(node: Node): Unit = if (!cache.containsKey(node.name))
      throw new IllegalArgumentException(s"${node.name} doesn't exist")
    else cache.put(node.name, node)

    /**
      * this way is faster.
      * @return size of stored nodes
      */
    override def size: Int = cache.size()
  }

  def inMemory(nodes: Seq[Node] = Seq.empty): NodeCollie = new NodeCollieImpl(nodes)
}
