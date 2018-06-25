package com.island.ohara.configurator.data

import com.island.ohara.config.{OharaConfig, OharaJson, OharaProperty}

/**
  * a basic type of ohara data object. It implements most methods used in the subclass. This class will valid all
  * properties in construction. If any properties don't exist, an IllegalArgumentException will be thrown.
  *
  * @param config stores all properties
  */
abstract class OharaData(private val config: OharaConfig) {
  val properties = Array[OharaProperty[_]](OharaData.uuid, OharaData.implName, OharaData.name) ++ extraProperties
  if (config.get(OharaData.implName.key).isEmpty) OharaData.implName.set(config, getClass.getName)
  // verify the content of passed config. Throw the exception if any property isn't included in the config
  if (properties == null || properties.isEmpty) throw new IllegalArgumentException("Must have property")
  properties.foreach(_.require(config))

  /**
    * @return uuid to this object
    */
  def uuid: String = OharaData.uuid.require(config)

  /**
    * @return the impl of ohara data
    */
  def impl: String = OharaData.implName.require(config)

  /**
    * @return the impl of ohara data
    */
  def name: String = OharaData.name.require(config)

  def copy[T](prop: OharaProperty[T], value: T): OharaData

  /**
    * Convert to json
    * @return json
    */
  def toJson: OharaJson = config.toJson

  override def equals(obj: scala.Any): Boolean = obj match {
    case another: OharaData => config.equals(another.config)
    case _                  => false
  }

  /**
    * @return the valid properties for this ohara data
    */
  protected def extraProperties: Seq[OharaProperty[_]]
}

object OharaData {

  val uuid: OharaProperty[String] =
    OharaProperty.builder.key("uuid").description("the uuid of ohara data").stringProperty
  val implName: OharaProperty[String] =
    OharaProperty.builder.key("implName").description("the impl of ohara data").stringProperty
  val name: OharaProperty[String] =
    OharaProperty.builder.key("name").description("the name of ohara data").stringProperty
}
