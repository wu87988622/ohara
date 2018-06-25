package com.island.ohara.configurator.data

import com.island.ohara.config.{OharaConfig, OharaProperty}

/**
  * a pojo to represent the description of ohara streaming
  * @param config stores all properties
  */
class OharaStreaming(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[OharaProperty[_]] = OharaStreaming.properties

  def schemaId: String = OharaStreaming.schemaId.require(config)

  def topicId: String = OharaStreaming.topicId.require(config)
  override def copy[T](prop: OharaProperty[T], value: T): OharaStreaming = {
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
    OharaData.uuid.set(oharaConfig, uuid)
    OharaData.name.set(oharaConfig, name)
    OharaStreaming.schemaId.set(oharaConfig, schemaId)
    OharaStreaming.topicId.set(oharaConfig, topicId)
    new OharaStreaming(oharaConfig)
  }
  def properties: Seq[OharaProperty[_]] = Array(topicId, schemaId)
  val schemaId: OharaProperty[String] =
    OharaProperty.builder.key("schema").description("the id of ohara streaming").stringProperty
  val topicId: OharaProperty[String] =
    OharaProperty.builder.key("topic").description("the id of ohara topic").stringProperty
}
