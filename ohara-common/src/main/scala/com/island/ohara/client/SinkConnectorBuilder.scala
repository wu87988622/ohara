package com.island.ohara.client

import com.island.ohara.client.ConnectorJson.{ConnectorRequest, ConnectorResponse}

import scala.collection.mutable

/**
  * Used to config and run the sink connector.
  */
abstract class SinkConnectorBuilder extends ConnectorBuilder {

  /**
    * set the topics in which you have interest.
    *
    * @param topicNames topics
    * @return this one
    */
  def topics(topicNames: Seq[String]): this.type = {
    this.topicNames = topicNames
    this
  }

  /**
    * send the request to create the sink connector.
    *
    * @return this one
    */
  def build(): ConnectorResponse = {
    checkArgument()
    if (config == null) config = new mutable.HashMap[String, String]()
    config += ("connector.class" -> clzName)
    config += ("topics" -> topicNames.mkString(","))
    config += ("tasks.max" -> taskMax.toString)
    if (_disableKeyConverter) config += ("key.converter" -> "org.apache.kafka.connect.converters.ByteArrayConverter")
    if (_disableValueConverter)
      config += ("value.converter" -> "org.apache.kafka.connect.converters.ByteArrayConverter")
    send(ConnectorRequest(name, config.toMap))
  }

  /**
    * send the request to kafka worker
    *
    * @param cmd related path
    * @param body body
    * @return response
    */
  protected def send(request: ConnectorRequest): ConnectorResponse
}
