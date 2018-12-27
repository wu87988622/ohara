package com.island.ohara.agent
import java.util.Objects

import com.island.ohara.client.ConfiguratorJson.ZookeeperClusterDescription
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.{Releasable, VersionUtil}

import scala.concurrent.Future

/**
  * a interface of controlling zookeeper cluster.
  * It isolates the implementation of container manager from Configurator.
  */
trait ZookeeperCollie extends Releasable with Collie[ZookeeperClusterDescription] {
  override def creator(): ZookeeperCollie.ClusterCreator
}

object ZookeeperCollie {
  trait ClusterCreator extends Collie.ClusterCreator[ZookeeperClusterDescription] {
    private[this] var clientPort: Int = ZookeeperCollie.CLIENT_PORT_DEFAULT
    private[this] var peerPort: Int = ZookeeperCollie.PEER_PORT_DEFAULT
    private[this] var electionPort: Int = ZookeeperCollie.ELECTION_PORT_DEFAULT

    /**
      * In ZookeeperRoute we accept the option arguments from restful APIs. This method help caller to apply fluent pattern.
      * @param clientPort client port
      * @return this creator
      */
    @Optional("default port is 2181")
    def clientPort(clientPort: Option[Int]): ClusterCreator = {
      clientPort.foreach(ClusterCreator.this.clientPort(_))
      this
    }
    @Optional("default port is 2181")
    def clientPort(clientPort: Int): ClusterCreator = {
      this.clientPort = clientPort
      this
    }

    /**
      * In ZookeeperRoute we accept the option arguments from restful APIs. This method help caller to apply fluent pattern.
      * @param peerPort peer port
      * @return this creator
      */
    @Optional("default port is 2888")
    def peerPort(peerPort: Option[Int]): ClusterCreator = {
      peerPort.foreach(ClusterCreator.this.peerPort(_))
      this
    }

    @Optional("default port is 2888")
    def peerPort(peerPort: Int): ClusterCreator = {
      this.peerPort = peerPort
      this
    }

    /**
      * In ZookeeperRoute we accept the option arguments from restful APIs. This method help caller to apply fluent pattern.
      * @param electionPort election port
      * @return this creator
      */
    @Optional("default port is 3888")
    def electionPort(electionPort: Option[Int]): ClusterCreator = {
      electionPort.foreach(ClusterCreator.this.electionPort(_))
      this
    }
    @Optional("default port is 3888")
    def electionPort(electionPort: Int): ClusterCreator = {
      this.electionPort = electionPort
      this
    }

    def create(nodeNames: Seq[String]): Future[ZookeeperClusterDescription] = doCreate(
      clusterName = Objects.requireNonNull(clusterName),
      imageName = Option(imageName).getOrElse(ZookeeperCollie.IMAGE_NAME_DEFAULT),
      clientPort = clientPort,
      peerPort = peerPort,
      electionPort = electionPort,
      nodeNames =
        if (nodeNames == null || nodeNames.isEmpty) throw new IllegalArgumentException("nodes can't be empty")
        else nodeNames
    )

    protected def doCreate(clusterName: String,
                           imageName: String,
                           clientPort: Int,
                           peerPort: Int,
                           electionPort: Int,
                           nodeNames: Seq[String]): Future[ZookeeperClusterDescription]
  }

  /**
    * This property is useless now since our zookeepr image has "fixed" at specified script.
    * see docker/zookeeper.release.dockerfile for more details
    */
  private[agent] val SCRIPT_NAME: String = "zk.sh"

  /**
    * ohara-it needs this property for testing.
    */
  private[ohara] val IMAGE_NAME_DEFAULT: String = s"islandsystems/zookeeper:${VersionUtil.VERSION}"

  private[agent] val CLIENT_PORT_KEY: String = "ZK_CLIENT_PORT"
  private[agent] val CLIENT_PORT_DEFAULT: Int = 2181

  private[agent] val PEER_PORT_KEY: String = "ZK_PEER_PORT"
  private[agent] val PEER_PORT_DEFAULT: Int = 2888

  private[agent] val ELECTION_PORT_KEY: String = "ZK_ELECTION_PORT"
  private[agent] val ELECTION_PORT_DEFAULT: Int = 3888

  private[agent] val DATA_DIRECTORY_KEY: String = "ZK_DATA_DIR"
  private[agent] val SERVERS_KEY: String = "ZK_SERVERS"
  private[agent] val ID_KEY: String = "ZK_ID"
}
