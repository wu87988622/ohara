package com.island.ohara.agent
import java.util.Objects

import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.{CommonUtil, VersionUtil}

import scala.concurrent.Future

/**
  * a interface of controlling zookeeper cluster.
  * It isolates the implementation of container manager from Configurator.
  */
trait ZookeeperCollie extends Collie[ZookeeperClusterInfo] {
  override def creator(): ZookeeperCollie.ClusterCreator
}

object ZookeeperCollie {
  trait ClusterCreator extends Collie.ClusterCreator[ZookeeperClusterInfo] {
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
      clientPort.foreach(this.clientPort = _)
      this
    }

    def clientPort(port: Int): ClusterCreator = clientPort(Some(port))

    /**
      * In ZookeeperRoute we accept the option arguments from restful APIs. This method help caller to apply fluent pattern.
      * @param peerPort peer port
      * @return this creator
      */
    @Optional("default port is 2888")
    def peerPort(peerPort: Option[Int]): ClusterCreator = {
      peerPort.foreach(this.peerPort = _)
      this
    }

    def peerPort(port: Int): ClusterCreator = peerPort(Some(port))

    /**
      * In ZookeeperRoute we accept the option arguments from restful APIs. This method help caller to apply fluent pattern.
      * @param electionPort election port
      * @return this creator
      */
    @Optional("default port is 3888")
    def electionPort(electionPort: Option[Int]): ClusterCreator = {
      electionPort.foreach(this.electionPort = _)
      this
    }
    def electionPort(port: Int): ClusterCreator = electionPort(Some(port))

    def create(nodeNames: Seq[String]): Future[ZookeeperClusterInfo] = doCreate(
      clusterName = Objects.requireNonNull(clusterName),
      imageName = Option(imageName).getOrElse(ZookeeperCollie.IMAGE_NAME_DEFAULT),
      clientPort = CommonUtil.requirePositiveInt(clientPort, () => "clientPort must be positive"),
      peerPort = CommonUtil.requirePositiveInt(peerPort, () => "peerPort must be positive"),
      electionPort = CommonUtil.requirePositiveInt(electionPort, () => "electionPort must be positive"),
      nodeNames =
        if (nodeNames == null || nodeNames.isEmpty) throw new IllegalArgumentException("nodes can't be empty")
        else nodeNames
    )

    protected def doCreate(clusterName: String,
                           imageName: String,
                           clientPort: Int,
                           peerPort: Int,
                           electionPort: Int,
                           nodeNames: Seq[String]): Future[ZookeeperClusterInfo]
  }

  /**
    * This property is useless now since our zookeepr image has "fixed" at specified script.
    * see docker/zookeeper.release.dockerfile for more details
    */
  private[agent] val SCRIPT_NAME: String = "zk.sh"

  /**
    * ohara-it needs this property for testing.
    */
  private[ohara] val IMAGE_NAME_DEFAULT: String = s"oharastream/zookeeper:${VersionUtil.VERSION}"

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
