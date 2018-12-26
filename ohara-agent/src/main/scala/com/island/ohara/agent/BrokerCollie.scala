package com.island.ohara.agent
import com.island.ohara.client.ConfiguratorJson.BrokerClusterDescription
import com.island.ohara.common.util.{Releasable, VersionUtil}

trait BrokerCollie extends Releasable with Collie[BrokerClusterDescription] {
  def creator(): BrokerCollie.ClusterCreator
}

object BrokerCollie {
  trait ClusterCreator extends Collie.ClusterCreator[BrokerClusterDescription] {
    def zookeeperClusterName(name: String): ClusterCreator
    def clientPort(clientPort: Int): ClusterCreator
    def create(nodeNames: Seq[String]): BrokerClusterDescription
  }

  /**
    * ohara-it needs this property for testing.
    */
  private[ohara] val IMAGE_NAME_DEFAULT: String = s"islandsystems/broker:${VersionUtil.VERSION}"
  private[agent] val ID_KEY: String = "BROKER_ID"
  private[agent] val DATA_DIRECTORY_KEY: String = "BROKER_DATA_DIR"
  private[agent] val ZOOKEEPERS_KEY: String = "BROKER_ZOOKEEPERS"

  private[agent] val CLIENT_PORT_KEY: String = "BROKER_CLIENT_PORT"
  private[agent] val CLIENT_PORT_DEFAULT: Int = 9092

  private[agent] val ADVERTISED_HOSTNAME_KEY: String = "BROKER_ADVERTISED_HOSTNAME"
  private[agent] val ADVERTISED_CLIENT_PORT_KEY: String = "BROKER_ADVERTISED_CLIENT_PORT"
}
