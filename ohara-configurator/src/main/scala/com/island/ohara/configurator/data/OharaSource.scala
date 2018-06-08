package com.island.ohara.configurator.data

import com.island.ohara.config.{OharaConfig, Property}

/**
  * a pojo to represent the description of ohara source
  * @param config stores all properties
  */
class OharaSource(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[Property[_]] = OharaSource.properties

  def configs: Map[String, String] = OharaSource.configProperty.require(config)
  override def copy[T](prop: Property[T], value: T): OharaSource = {
    val clone = config.snapshot
    prop.set(clone, value)
    new OharaSource(clone)
  }
}

object OharaSource {

  /**
    * create a OharaSource with specified config
    * @param config config
    * @return a new OharaSource
    */
  def apply(config: OharaConfig) = new OharaSource(config)

  /**
    * create an new OharaSource with specified arguments
    * @param uuid uuid
    * @param name source name
    * @param configs configuration
    * @return an new OharaSource
    */
  def apply(uuid: String, name: String, configs: Map[String, String]): OharaSource = {
    val oharaConfig = OharaConfig()
    OharaData.uuidProperty.set(oharaConfig, uuid)
    OharaData.nameProperty.set(oharaConfig, name)
    configProperty.set(oharaConfig, configs)
    new OharaSource(oharaConfig)
  }
  def properties: Seq[Property[_]] = Array(configProperty)
  val configProperty: Property[Map[String, String]] =
    Property.builder.key("ohara-source-config").alias("config").description("the configs of ohara source").mapProperty
}
