package com.island.ohara.data

import com.island.ohara.config.{OharaConfig, OharaProperty}

import OharaStreaming._
import OharaData._

/**
  * a pojo to represent the description of ohara streaming
  * @param config stores all properties
  */
class OharaStreaming(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[OharaProperty[_]] = PROPERTIES

  def schemaId: String = SCHEMA_ID.require(config)

  def topicId: String = TOPIC_ID.require(config)
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
    UUID.set(oharaConfig, uuid)
    NAME.set(oharaConfig, name)
    SCHEMA_ID.set(oharaConfig, schemaId)
    TOPIC_ID.set(oharaConfig, topicId)
    new OharaStreaming(oharaConfig)
  }
  val SCHEMA_ID: OharaProperty[String] =
    OharaProperty.builder.key("schema").description("the id of ohara streaming").stringProperty
  val TOPIC_ID: OharaProperty[String] =
    OharaProperty.builder.key("topic").description("the id of ohara topic").stringProperty
  val PROPERTIES: Seq[OharaProperty[_]] = Array(TOPIC_ID, SCHEMA_ID)
}
