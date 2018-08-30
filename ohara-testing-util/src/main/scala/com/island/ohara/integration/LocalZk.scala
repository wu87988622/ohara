package com.island.ohara.integration

import java.net.InetSocketAddress

import com.island.ohara.io.{CloseOnce, IoUtil}
import org.apache.zookeeper.server.{NIOServerCnxnFactory, ZooKeeperServer}

/**
  * A standalone zk service. The data are located at {TMP}/standalone-zk. The default port is an random integer.
  *
  * @param _port    The port to bind. default is a random number
  * @param tickTime time to tick
  */
class LocalZk(_port: Int = -1, tickTime: Int = 500) extends CloseOnce {
  private[this] val port = if (_port <= 0) availablePort else _port
  private[this] val factory = new NIOServerCnxnFactory()
  private[this] val snapshotDir = createTempDir("standalone-zk/snapshot")
  private[this] val logDir = createTempDir("standalone-zk/log")
  factory.configure(new InetSocketAddress("0.0.0.0", port), 1024)
  factory.startup(new ZooKeeperServer(snapshotDir, logDir, tickTime))

  override protected def doClose(): Unit = {
    factory.shutdown()
    if (!deleteFile(snapshotDir)) throw new IllegalStateException(s"Fail to delete ${snapshotDir.getAbsolutePath}")
    if (!deleteFile(logDir)) throw new IllegalStateException(s"Fail to delete ${logDir.getAbsolutePath}")
  }

  /**
    * zookeeper connection information. The form is "IoUtil.hostname:{port}".
    * @return zk connection information
    */
  def connection: String = s"${IoUtil.hostname}:$port"

  override def toString: String = {
    val sb = new StringBuilder("EmbeddedZookeeper{")
    sb.append("connection=").append(connection)
    sb.append('}')
    sb.toString
  }
}
