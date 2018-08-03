package com.island.ohara.configurator.job

import com.island.ohara.config.{OharaConfig, OharaProperty, UuidUtil}
import com.island.ohara.data.OharaData
import com.island.ohara.serialization.DataType

import HttpJobRequest._
import OharaData._
class HttpJobRequest(oharaConfig: OharaConfig) extends OharaData(oharaConfig) {

  override def copy[T](prop: OharaProperty[T], value: T): HttpJobRequest = {
    val clone = oharaConfig.snapshot
    prop.set(clone, value)
    new HttpJobRequest(clone)
  }

  def path: String = PATH.require(oharaConfig)

  /**
    * @return action
    */
  def action: Action = ACTION.require(oharaConfig)

  /**
    * @return schema
    */
  def schema: Map[String, DataType] = SCHEMA.require(oharaConfig)

  /**
    * @return config
    */
  def config: Map[String, String] = CONFIG.require(oharaConfig)
  override protected def extraProperties: Seq[OharaProperty[_]] = PROPERTIES
}

object HttpJobRequest {

  def apply(action: Action, path: String, schema: Map[String, DataType], config: Map[String, String]): HttpJobRequest =
    apply(UuidUtil.uuid(), classOf[HttpJobRequest].getSimpleName, action, path, schema, config)

  def apply(uuid: String,
            name: String,
            action: Action,
            path: String,
            schema: Map[String, DataType],
            config: Map[String, String]): HttpJobRequest = {
    val oharaConfig = OharaConfig()
    UUID.set(oharaConfig, uuid)
    NAME.set(oharaConfig, name)
    ACTION.set(oharaConfig, action)
    PATH.set(oharaConfig, path)
    SCHEMA.set(oharaConfig, schema)
    CONFIG.set(oharaConfig, config)
    new HttpJobRequest(oharaConfig)
  }

  val PATH: OharaProperty[String] =
    OharaProperty.builder.key("path").description("the path of HttpJobRequest").stringProperty
  val ACTION: OharaProperty[Action] =
    OharaProperty.builder.key("action").description("the action of HttpJobRequest").property(Action.of(_), _.name)
  val SCHEMA: OharaProperty[Map[String, DataType]] =
    OharaProperty.builder.key("schema").description("the schema of HttpJobRequest").mapProperty(DataType.of(_), _.name)

  val CONFIG: OharaProperty[Map[String, String]] =
    OharaProperty.builder.key("config").description("the config of HttpJobRequest").mapProperty
  val PROPERTIES: Seq[OharaProperty[_]] = Array(PATH, ACTION, CONFIG, SCHEMA)

}
