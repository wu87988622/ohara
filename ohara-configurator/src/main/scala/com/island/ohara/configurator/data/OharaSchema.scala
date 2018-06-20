package com.island.ohara.configurator.data

import com.island.ohara.config.{OharaConfig, OharaProperty}
import com.island.ohara.serialization.DataType

/**
  * a pojo to represent the description of ohara job
  * @param config stores all properties
  */
class OharaSchema(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[OharaProperty[_]] = OharaSchema.properties

  def types: Map[String, DataType] = OharaSchema.columnType.require(config)
  def indexes: Map[String, Int] = OharaSchema.indexType.require(config)

  override def copy[T](prop: OharaProperty[T], value: T): OharaSchema = {
    val clone = config.snapshot
    prop.set(clone, value)
    new OharaSchema(clone)
  }
}

object OharaSchema {

  /**
    * create a OharaSchema with specified config
    * @param config config
    * @return a new OharaSchema
    */
  def apply(config: OharaConfig) = new OharaSchema(config)

  /**
    * create an new OharaSchema with specified arguments
    * @param uuid uuid
    * @param name target name
    * @param columns columnName-type
    * @return an new OharaSchema
    */
  def apply(uuid: String, name: String, columns: Map[String, DataType], indexes: Map[String, Int]): OharaSchema = {
    val oharaConfig = OharaConfig()
    OharaData.uuidProperty.set(oharaConfig, uuid)
    OharaData.nameProperty.set(oharaConfig, name)
    columnType.set(oharaConfig, columns)
    indexType.set(oharaConfig, indexes)
    new OharaSchema(oharaConfig)
  }

  def properties: Seq[OharaProperty[_]] = Array(columnType)

  val columnType: OharaProperty[Map[String, DataType]] = OharaProperty.builder
    .key("ohara-schema-columns")
    .alias("columns")
    .description("the columns of ohara schema")
    .mapProperty(DataType.of(_), _.name)

  val indexType: OharaProperty[Map[String, Int]] = OharaProperty.builder
    .key("ohara-schema-index")
    .alias("index")
    .description("the index of ohara schema")
    .mapProperty(_.toInt, _.toString)
}
