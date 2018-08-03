package com.island.ohara.data

import java.util.concurrent.atomic.AtomicLong

import com.island.ohara.config.{OharaConfig, OharaJson, OharaProperty}
import OharaData._

/**
  * a basic type of ohara data object. It implements most methods used in the subclass. This class will valid all
  * properties in construction. If any properties don't exist, an IllegalArgumentException will be thrown.
  *
  * @param config stores all properties
  * @param defaultUuid true if both of name and uuid are unnecessary
  */
abstract class OharaData(private val config: OharaConfig, defaultUuid: Boolean = false) {
  if (defaultUuid) {
    if (!UUID.exist(config)) UUID.set(config, INDEXER.getAndIncrement().toString)
    if (!NAME.exist(config)) NAME.set(config, getClass.getSimpleName)
  }
  val properties = Array[OharaProperty[_]](UUID, IMPLEMENTATION_NAME, NAME) ++ extraProperties
  if (config.get(OharaData.IMPLEMENTATION_NAME.key).isEmpty) OharaData.IMPLEMENTATION_NAME.set(config, getClass.getName)
  // verify the content of passed config. Throw the exception if any property isn't included in the config
  if (properties == null || properties.isEmpty) throw new IllegalArgumentException("Must have property")
  properties.foreach(_.require(config))

  /**
    * @return uuid to this object
    */
  def uuid: String = OharaData.UUID.require(config)

  /**
    * @return the impl of ohara data
    */
  def impl: String = OharaData.IMPLEMENTATION_NAME.require(config)

  /**
    * @return the impl of ohara data
    */
  def name: String = OharaData.NAME.require(config)

  def copy[T](prop: OharaProperty[T], value: T): OharaData

  /**
    * Convert to json
    * @return json
    */
  def toJson: OharaJson = config.toJson

  override def equals(obj: scala.Any): Boolean = equals(obj, true)

  def equals(obj: scala.Any, includeUuid: Boolean): Boolean = obj match {
    case another: OharaData =>
      if (!includeUuid) {
        val configCopy = OharaConfig(config)
        configCopy.remove(OharaData.UUID.key)
        val anotherConfigCopy = OharaConfig(config)
        anotherConfigCopy.remove(OharaData.UUID.key)
        configCopy.equals(anotherConfigCopy)
      } else config.equals(another.config)
    case _ => false
  }

  /**
    * @return the valid properties for this ohara data
    */
  protected def extraProperties: Seq[OharaProperty[_]]

  override def toString(): String = config.toString()
}

object OharaData {

  /**
    * a auto-increment number. We don't care about the uuid of OharaRequest.
    */
  private val INDEXER = new AtomicLong(0)

  val UUID: OharaProperty[String] =
    OharaProperty.builder.key("uuid").description("the uuid of ohara data").stringProperty
  val IMPLEMENTATION_NAME: OharaProperty[String] =
    OharaProperty.builder.key("implementationName").description("the impl of ohara data").stringProperty
  val NAME: OharaProperty[String] =
    OharaProperty.builder.key("name").description("the name of ohara data").stringProperty
}
