package com.island.ohara.integration
import java.net.InetSocketAddress

import com.island.ohara.io.{CloseOnce, IoUtil}
import org.apache.zookeeper.server.{NIOServerCnxnFactory, ZooKeeperServer}

trait Zookeepers extends CloseOnce {

  /**
    * @return zookeeper information. the form is "host_a:port_a,host_b:port_b"
    */
  def connectionProps: String

  /**
    * @return true if this zookeeper cluster is generated locally.
    */
  def isLocal: Boolean
}

object Zookeepers {
  private[integration] val ZOOKEEPER_CONNECTION_PROPS: String = "ohara.it.zookeepers"
  def apply(): Zookeepers = apply(sys.env.get(ZOOKEEPER_CONNECTION_PROPS))

  private[integration] def apply(zookeepers: Option[String]): Zookeepers = zookeepers
    .map { s =>
      new Zookeepers {
        override def connectionProps: String = s
        override protected def doClose(): Unit = {}
        override def isLocal: Boolean = false
      }
    }
    .getOrElse {
      val port: Int = availablePort()
      val factory = new NIOServerCnxnFactory()
      val snapshotDir = createTempDir("standalone-zk/snapshot")
      val logDir = createTempDir("standalone-zk/log")
      factory.configure(new InetSocketAddress("0.0.0.0", port), 1024)
      factory.startup(new ZooKeeperServer(snapshotDir, logDir, 500))

      new Zookeepers {
        override def connectionProps: String = s"${IoUtil.hostname}:$port"

        override protected def doClose(): Unit = {
          factory.shutdown()
          if (!deleteFile(snapshotDir))
            throw new IllegalStateException(s"Fail to delete ${snapshotDir.getAbsolutePath}")
          if (!deleteFile(logDir)) throw new IllegalStateException(s"Fail to delete ${logDir.getAbsolutePath}")
        }
        override def isLocal: Boolean = true
      }
    }
}
