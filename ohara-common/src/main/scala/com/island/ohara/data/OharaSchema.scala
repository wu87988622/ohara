package com.island.ohara.data

import com.island.ohara.config.{OharaConfig, OharaJson, OharaProperty}
import com.island.ohara.serialization.DataType

import OharaSchema._
import OharaData._

/**
  * a pojo to represent the description of ohara job
  * @param config stores all properties
  */
class OharaSchema(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[OharaProperty[_]] = OharaSchema.PROPERTIES

  def types: Map[String, DataType] = COLUMN_TYPE.require(config)
  def orders: Map[String, Int] = COLUMN_ORDER.require(config)

  def disabled: Boolean = DISABLED.require(config)

  override def copy[T](prop: OharaProperty[T], value: T): OharaSchema = {
    val clone = config.snapshot
    prop.set(clone, value)
    new OharaSchema(clone)
  }
}

object OharaSchema {

  /**
    * Create the a ohara schema in json format. This helper method is used to sent the schema request to rest server.
    * NOTED: it is used in testing only
    * @param name name
    * @param types column types
    * @param orders column orders
    * @return json
    */
  def json(name: String, types: Map[String, DataType], orders: Map[String, Int], disabled: Boolean): OharaJson = {
    val config = OharaConfig()
    NAME.set(config, name)
    COLUMN_TYPE.set(config, types)
    COLUMN_ORDER.set(config, orders)
    DISABLED.set(config, disabled)
    config.toJson
  }

  /**
    * create a OharaSchema with specified config
    * @param json config in json format
    * @return a new OharaSchema
    */
  def apply(json: OharaJson): OharaSchema = apply(OharaConfig(json))

  /**
    * create a OharaSchema with specified config
    * @param config config
    * @return a new OharaSchema
    */
  def apply(config: OharaConfig): OharaSchema = new OharaSchema(config)

  /**
    * create an new OharaSchema with specified arguments
    * @param uuid uuid
    * @param json remaining options in json format
    * @return an new OharaSchema
    */
  def apply(uuid: String, json: OharaJson): OharaSchema = {
    val oharaConfig = OharaConfig(json)
    UUID.set(oharaConfig, uuid)
    new OharaSchema(oharaConfig)
  }

  /**
    * create an new OharaSchema with specified arguments
    * @param uuid uuid
    * @param name target name
    * @param types columnName-type
    * @return an new OharaSchema
    */
  def apply(uuid: String,
            name: String,
            types: Map[String, DataType],
            orders: Map[String, Int],
            disabled: Boolean): OharaSchema = {
    val oharaConfig = OharaConfig()
    UUID.set(oharaConfig, uuid)
    NAME.set(oharaConfig, name)
    COLUMN_TYPE.set(oharaConfig, types)
    COLUMN_ORDER.set(oharaConfig, orders)
    DISABLED.set(oharaConfig, disabled)
    new OharaSchema(oharaConfig)
  }

  val COLUMN_TYPE: OharaProperty[Map[String, DataType]] =
    OharaProperty.builder
      .key("types")
      .description("the column type of ohara schema")
      .mapProperty(DataType.of(_), _.name)

  val COLUMN_ORDER: OharaProperty[Map[String, Int]] =
    OharaProperty.builder.key("orders").description("the column order of ohara schema").mapProperty(_.toInt, _.toString)

  val DISABLED: OharaProperty[Boolean] =
    OharaProperty.builder
      .key("disabled")
      .description("true if this schema is selectable in UI. otherwise false")
      .booleanProperty

  val PROPERTIES: Seq[OharaProperty[_]] = Array(COLUMN_TYPE, COLUMN_ORDER, DISABLED)
}
