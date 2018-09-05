package com.island.ohara.client

import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.client.ConnectorJson.{CreateConnectorRequest, CreateConnectorResponse}

import scala.collection.mutable

/**
  * Used to config and run the source connector.
  */
abstract class SourceConnectorBuilder extends ConnectorBuilder {
  private[this] var schema: Seq[Column] = _

  def schema(schema: Seq[Column]): SourceConnectorBuilder = {
    this.schema = schema
    this
  }

  /**
    * send the request to create the sink connector.
    * @return this one
    */
  override def build(): CreateConnectorResponse = {
    checkArgument()
    if (topicNames.size != 1) throw new IllegalArgumentException("multi-topics is invalid for source connector")
    if (config == null) config = new mutable.HashMap[String, String]()
    config += ("connector.class" -> clzName)
    // NOTED: the key "topics" is mapped to RowSourceConnector.TOPICS_KEYS
    config += ("topics" -> topicNames.mkString(","))
    config += ("tasks.max" -> numberOfTasks.toString)
    if (schema != null) config += (Column.COLUMN_KEY -> Column.toString(schema))
    if (_disableKeyConverter) config += ("key.converter" -> "org.apache.kafka.connect.converters.ByteArrayConverter")
    if (_disableValueConverter)
      config += ("value.converter" -> "org.apache.kafka.connect.converters.ByteArrayConverter")
    send(CreateConnectorRequest(name, config.toMap))
  }

  /**
    * send the request to kafka worker
    * @param request request
    * @return response
    */
  protected def send(request: CreateConnectorRequest): CreateConnectorResponse
}
