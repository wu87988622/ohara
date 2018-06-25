package com.island.ohara.configurator.data

import com.island.ohara.config.{OharaConfig, OharaJson, OharaProperty}
import com.island.ohara.serialization.DataType

/**
  * a pojo to represent the description of ohara job
  * @param config stores all properties
  */
class OharaSchema(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[OharaProperty[_]] = OharaSchema.properties

  def types: Map[String, DataType] = OharaSchema.columnType.require(config)
  def indexes: Map[String, Int] = OharaSchema.columnIndex.require(config)

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
    * @param indexes column indexes
    * @return json
    */
  private[configurator] def json(name: String, types: Map[String, DataType], indexes: Map[String, Int]): OharaJson = {
    val config = OharaConfig()
    OharaData.name.set(config, name)
    columnType.set(config, types)
    columnIndex.set(config, indexes)
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
    * @param name target name
    * @param columns columnName-type
    * @return an new OharaSchema
    */
  def apply(uuid: String, name: String, columns: Map[String, DataType], indexes: Map[String, Int]): OharaSchema = {
    val oharaConfig = OharaConfig()
    OharaData.uuid.set(oharaConfig, uuid)
    OharaData.name.set(oharaConfig, name)
    columnType.set(oharaConfig, columns)
    columnIndex.set(oharaConfig, indexes)
    new OharaSchema(oharaConfig)
  }

  def properties: Seq[OharaProperty[_]] = Array(columnType)

  val columnType: OharaProperty[Map[String, DataType]] =
    OharaProperty.builder.key("type").description("the column type of ohara schema").mapProperty(DataType.of(_), _.name)

  val columnIndex: OharaProperty[Map[String, Int]] =
    OharaProperty.builder.key("index").description("the column index of ohara schema").mapProperty(_.toInt, _.toString)
}
