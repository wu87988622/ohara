package com.island.ohara.configurator.job

import com.island.ohara.config.{OharaConfig, OharaProperty, UuidUtil}
import com.island.ohara.configurator.data.OharaData
import com.island.ohara.serialization.DataType

class HttpJobRequest(oharaConfig: OharaConfig) extends OharaData(oharaConfig) {

  override def copy[T](prop: OharaProperty[T], value: T): HttpJobRequest = {
    val clone = oharaConfig.snapshot
    prop.set(clone, value)
    new HttpJobRequest(clone)
  }

  def path: String = HttpJobRequest.path.require(oharaConfig)

  /**
    * @return action
    */
  def action: Action = HttpJobRequest.action.require(oharaConfig)

  /**
    * @return schema
    */
  def schema: Map[String, DataType] = HttpJobRequest.schema.require(oharaConfig)

  /**
    * @return config
    */
  def config: Map[String, String] = HttpJobRequest.config.require(oharaConfig)
  override protected def extraProperties: Seq[OharaProperty[_]] = HttpJobRequest.properties
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
    OharaData.uuid.set(oharaConfig, uuid)
    OharaData.name.set(oharaConfig, name)
    HttpJobRequest.action.set(oharaConfig, action)
    HttpJobRequest.path.set(oharaConfig, path)
    HttpJobRequest.schema.set(oharaConfig, schema)
    HttpJobRequest.config.set(oharaConfig, config)
    new HttpJobRequest(oharaConfig)
  }

  def properties: Seq[OharaProperty[_]] = Array(path, action, config, schema)
  val path: OharaProperty[String] =
    OharaProperty.builder.key("path").description("the path of HttpJobRequest").stringProperty
  val action: OharaProperty[Action] =
    OharaProperty.builder.key("action").description("the action of HttpJobRequest").property(Action.of(_), _.name)
  val schema: OharaProperty[Map[String, DataType]] =
    OharaProperty.builder.key("schema").description("the schema of HttpJobRequest").mapProperty(DataType.of(_), _.name)

  val config: OharaProperty[Map[String, String]] =
    OharaProperty.builder.key("config").description("the config of HttpJobRequest").mapProperty
}
