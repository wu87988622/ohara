package com.island.ohara.data

import com.island.ohara.config.{OharaConfig, OharaProperty}

import OharaTarget._
import OharaData._

/**
  * a pojo to represent the description of ohara target
  * @param config stores all properties
  */
class OharaTarget(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[OharaProperty[_]] = PROPERTIES

  def configs: Map[String, String] = CONFIG.require(config)

  override def copy[T](prop: OharaProperty[T], value: T): OharaTarget = {
    val clone = config.snapshot
    prop.set(clone, value)
    new OharaTarget(clone)
  }
}

object OharaTarget {

  /**
    * create a OharaTarget with specified config
    * @param config config
    * @return a new OharaTarget
    */
  def apply(config: OharaConfig) = new OharaTarget(config)

  /**
    * create an new OharaTarget with specified arguments
    * @param uuid uuid
    * @param name target name
    * @param config configuration
    * @return an new OharaTarget
    */
  def apply(uuid: String, name: String, config: Map[String, String]): OharaTarget = {
    val oharaConfig = OharaConfig()
    UUID.set(oharaConfig, uuid)
    NAME.set(oharaConfig, name)
    CONFIG.set(oharaConfig, config)
    new OharaTarget(oharaConfig)
  }
  val CONFIG: OharaProperty[Map[String, String]] =
    OharaProperty.builder.key("config").description("the configs of ohara target").mapProperty
  val PROPERTIES: Seq[OharaProperty[_]] = Array(CONFIG)

}
