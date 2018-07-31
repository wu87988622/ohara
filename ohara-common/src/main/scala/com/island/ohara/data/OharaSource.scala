package com.island.ohara.data

import com.island.ohara.config.{OharaConfig, OharaProperty}

/**
  * a pojo to represent the description of ohara source
  * @param config stores all properties
  */
class OharaSource(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[OharaProperty[_]] = OharaSource.properties

  def configs: Map[String, String] = OharaSource.config.require(config)
  override def copy[T](prop: OharaProperty[T], value: T): OharaSource = {
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
    * @param config configuration
    * @return an new OharaSource
    */
  def apply(uuid: String, name: String, config: Map[String, String]): OharaSource = {
    val oharaConfig = OharaConfig()
    OharaData.uuid.set(oharaConfig, uuid)
    OharaData.name.set(oharaConfig, name)
    OharaSource.config.set(oharaConfig, config)
    new OharaSource(oharaConfig)
  }
  def properties: Seq[OharaProperty[_]] = Array(config)
  val config: OharaProperty[Map[String, String]] =
    OharaProperty.builder.key("config").description("the config of ohara source").mapProperty
}
