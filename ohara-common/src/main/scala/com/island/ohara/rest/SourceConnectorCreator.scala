package com.island.ohara.rest

import com.island.ohara.rest.ConnectorJson.{ConnectorRequest, ConnectorResponse}

import scala.collection.mutable

/**
  * Used to config and run the source connector.
  */
abstract class SourceConnectorCreator extends ConnectorCreator {

  /**
    * send the request to create the sink connector.
    * @return this one
    */
  override def run(): ConnectorResponse = {
    checkArgument()
    if (topicNames.size != 1) throw new IllegalArgumentException("multi-topics is invalid for source connector")
    if (config == null) config = new mutable.HashMap[String, String]()
    config += ("connector.class" -> clzName)
    config += ("topic" -> topicNames(0))
    config += ("tasks.max" -> taskMax.toString)
    if (_disableKeyConverter) config += ("key.converter" -> "org.apache.kafka.connect.converters.ByteArrayConverter")
    if (_disableValueConverter)
      config += ("value.converter" -> "org.apache.kafka.connect.converters.ByteArrayConverter")
    send(ConnectorRequest(name, config.toMap))
  }

  /**
    * send the request to kafka worker
    * @param request request
    * @return response
    */
  protected def send(request: ConnectorRequest): ConnectorResponse
}
