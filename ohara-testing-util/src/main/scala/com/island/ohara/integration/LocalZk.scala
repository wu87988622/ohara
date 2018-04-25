package com.island.ohara.integration

import java.net.InetSocketAddress

import com.island.ohara.io.CloseOnce
import org.apache.zookeeper.server.{NIOServerCnxnFactory, ZooKeeperServer}

/**
  * A standalone zk service.
  *
  * @param _port    The port to bind. default is a random number
  * @param tickTime time to tick
  */
private class LocalZk(_port: Int = -1, tickTime: Int = 500) extends CloseOnce {
  private[this] val port = if (_port <= 0) availablePort else _port
  private[this] val factory = new NIOServerCnxnFactory()
  private[this] val snapshotDir = createTempDir("standalone-zk/snapshot")
  private[this] val logDir = createTempDir("standalone-zk/log")
  factory.configure(new InetSocketAddress("localhost", port), 1024)
  factory.startup(new ZooKeeperServer(snapshotDir, logDir, tickTime))

  override protected def doClose(): Unit = {
    factory.shutdown()
    deleteFile(snapshotDir)
    deleteFile(logDir)
  }

  def connection: String = "localhost:" + port

  override def toString: String = {
    val sb = new StringBuilder("EmbeddedZookeeper{")
    sb.append("connection=").append(connection)
    sb.append('}')
    sb.toString
  }
}
