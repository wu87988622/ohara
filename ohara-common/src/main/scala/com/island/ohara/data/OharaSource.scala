package com.island.ohara.data

import com.island.ohara.config.{OharaConfig, OharaProperty}

import OharaSource._
import OharaData._

/**
  * a pojo to represent the description of ohara source
  * @param config stores all properties
  */
class OharaSource(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[OharaProperty[_]] = PROPERTIES

  def configs: Map[String, String] = CONFIG.require(config)
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
    UUID.set(oharaConfig, uuid)
    NAME.set(oharaConfig, name)
    CONFIG.set(oharaConfig, config)
    new OharaSource(oharaConfig)
  }
  val CONFIG: OharaProperty[Map[String, String]] =
    OharaProperty.builder.key("config").description("the config of ohara source").mapProperty
  val PROPERTIES: Seq[OharaProperty[_]] = Array(CONFIG)

}
