package com.island.ohara.integration

import org.apache.kafka.connect.sink.SinkConnector

/**
  * Used to config and run the sink connector.
  */
trait SinkConnectorCreator extends com.island.ohara.rest.SinkConnectorCreator {

  def connectorClass(clz: Class[_ <: SinkConnector]): SinkConnectorCreator = connectorClass(clz.getName)
}
