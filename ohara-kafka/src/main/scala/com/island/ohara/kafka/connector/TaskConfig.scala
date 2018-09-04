package com.island.ohara.kafka.connector
import com.island.ohara.client.ConfiguratorJson.Column

/**
  * this class carries all required configs for row connectors.
  * @param topics target topics which row source task should sent data
  * @param schema row schema
  * @param options other configs
  */
case class TaskConfig(topics: Seq[String], schema: Seq[Column], options: Map[String, String])
