package com.island.ohara.configurator.data

import com.island.ohara.config.{OharaConfig, Property}

/**
  * a pojo to represent the description of ohara streaming
  * @param config stores all properties
  */
class OharaStreaming(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[Property[_]] = OharaStreaming.properties

  def schemaId: String = OharaStreaming.schemaIdProperty.require(config)

  def topicId: String = OharaStreaming.topicIdProperty.require(config)
  override def copy[T](prop: Property[T], value: T): OharaStreaming = {
    val clone = config.snapshot
    prop.set(clone, value)
    new OharaStreaming(clone)
  }
}

object OharaStreaming {

  /**
    * create a OharaStreaming with specified config
    * @param config config
    * @return a new OharaStreaming
    */
  def apply(config: OharaConfig) = new OharaStreaming(config)

  /**
    * create an new OharaStreaming with specified arguments
    * @param uuid uuid
    * @param name target name
    * @param schemaId uuid of scheam
    * @param topicId uuid of topic
    * @return an new OharaStreaming
    */
  def apply(uuid: String, name: String, schemaId: String, topicId: String): OharaStreaming = {
    val oharaConfig = OharaConfig()
    OharaData.uuidProperty.set(oharaConfig, uuid)
    OharaData.nameProperty.set(oharaConfig, name)
    schemaIdProperty.set(oharaConfig, schemaId)
    topicIdProperty.set(oharaConfig, topicId)
    new OharaStreaming(oharaConfig)
  }
  def properties: Seq[Property[_]] = Array(topicIdProperty, schemaIdProperty)
  val schemaIdProperty: Property[String] = Property.builder
    .key("ohara-streaming-schema-id")
    .alias("schema")
    .description("the id of ohara streaming")
    .stringProperty
  val topicIdProperty: Property[String] =
    Property.builder.key("ohara-streaming-topic-id").alias("topic").description("the id of ohara topic").stringProperty
}
