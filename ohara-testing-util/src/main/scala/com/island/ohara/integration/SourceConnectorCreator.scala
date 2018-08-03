package com.island.ohara.integration

import org.apache.kafka.connect.source.SourceConnector

/**
  * Used to config and run the source connector.
  */
trait SourceConnectorCreator extends com.island.ohara.rest.SourceConnectorCreator {

  /**
    * set the connector class. The class must be loaded in class loader otherwise it will fail to create the connector.
    * @param clz connector class
    * @return this one
    */
  def connectorClass(clz: Class[_ <: SourceConnector]): SourceConnectorCreator = connectorClass(clz.getName)
}
