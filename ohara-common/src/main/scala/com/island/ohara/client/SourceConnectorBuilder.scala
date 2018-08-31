package com.island.ohara.client

import com.island.ohara.client.ConnectorJson.{ConnectorRequest, ConnectorResponse}

import scala.collection.mutable

/**
  * Used to config and run the source connector.
  */
abstract class SourceConnectorBuilder extends ConnectorBuilder {

  /**
    * send the request to create the sink connector.
    * @return this one
    */
  override def build(): ConnectorResponse = {
    checkArgument()
    if (topicNames.size != 1) throw new IllegalArgumentException("multi-topics is invalid for source connector")
    if (config == null) config = new mutable.HashMap[String, String]()
    config += ("connector.class" -> clzName)
    config += ("topic" -> topicNames.head)
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
