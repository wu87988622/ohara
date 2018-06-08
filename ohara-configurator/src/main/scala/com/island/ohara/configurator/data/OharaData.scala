package com.island.ohara.configurator.data

import com.island.ohara.config.{OharaConfig, OharaJson, Property}

/**
  * a basic type of ohara data object. It implements most methods used in the subclass. This class will valid all
  * properties in construction. If any properties don't exist, an IllegalArgumentException will be thrown.
  *
  * @param config stores all properties
  */
abstract class OharaData(private val config: OharaConfig) {
  val properties = Array[Property[_]](OharaData.uuidProperty, OharaData.implProperty, OharaData.nameProperty) ++ extraProperties
  if (config.get(OharaData.implProperty.key).isEmpty) OharaData.implProperty.set(config, getClass.getName)
  // verify the content of passed config. Throw the exception if any property isn't included in the config
  if (properties == null || properties.isEmpty) throw new IllegalArgumentException("Must have property")
  properties.foreach(_.require(config))

  /**
    * @return uuid to this object
    */
  def uuid: String = OharaData.uuidProperty.require(config)

  /**
    * @return the impl of ohara data
    */
  def impl: String = OharaData.implProperty.require(config)

  /**
    * @return the impl of ohara data
    */
  def name: String = OharaData.nameProperty.require(config)

  def copy[T](prop: Property[T], value: T): OharaData

  /**
    * Convert to json
    * @param alias true if you want to replace the key by the alias.
    * @return json
    */
  def toJson(alias: Boolean = true): OharaJson = {
    if (alias) {
      config.toJson
    } else config.toJson
  }

  override def equals(obj: scala.Any): Boolean = obj match {
    case another: OharaData => config.equals(another.config)
    case _                  => false
  }

  /**
    * @return the valid properties for this ohara data
    */
  protected def extraProperties: Seq[Property[_]]
}

object OharaData {
  val uuidProperty =
    Property.builder.key("ohara-data-uuid").alias("uuid").description("the uuid of ohara data").stringProperty
  val implProperty =
    Property.builder.key("ohara-data-impl").alias("impl").description("the impl of ohara data").stringProperty
  val nameProperty =
    Property.builder.key("ohara-data-name").alias("name").description("the name of ohara data").stringProperty
}
