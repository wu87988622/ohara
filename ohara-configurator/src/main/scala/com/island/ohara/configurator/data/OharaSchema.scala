package com.island.ohara.configurator.data

import com.island.ohara.config.{OharaConfig, Property}
import com.island.ohara.serialization.DataType

/**
  * a pojo to represent the description of ohara job
  * @param config stores all properties
  */
class OharaSchema(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[Property[_]] = OharaSchema.properties

  def columns: Map[String, DataType] = OharaSchema.columnsProperty.require(config)
  override def copy[T](prop: Property[T], value: T): OharaSchema = {
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
  def apply(uuid: String, name: String, columns: Map[String, DataType]): OharaSchema = {
    val oharaConfig = OharaConfig()
    OharaData.uuidProperty.set(oharaConfig, uuid)
    OharaData.nameProperty.set(oharaConfig, name)
    columnsProperty.set(oharaConfig, columns)
    new OharaSchema(oharaConfig)
  }
  def properties: Seq[Property[_]] = Array(columnsProperty)
  val columnsProperty: Property[Map[String, DataType]] = Property.builder
    .key("ohara-schema-columns")
    .alias("columns")
    .description("the columns of ohara schema")
    .mapProperty(DataType.of(_), _.name)
}
