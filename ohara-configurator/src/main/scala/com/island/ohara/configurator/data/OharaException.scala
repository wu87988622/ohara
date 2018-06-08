package com.island.ohara.configurator.data

import java.util.concurrent.atomic.AtomicLong

import com.island.ohara.config.{OharaConfig, Property}
import org.apache.commons.lang3.exception.ExceptionUtils

/**
  * Represent a remote exception. It can carray the error stack (in string) and the description of exception.
  * If server encounter something unhandleable error, the OharaExcpeiotn will be created and be a response.
  * Usually, user won't create this object.
  * @param config stores all properties
  */
class OharaException(config: OharaConfig) extends OharaData(config) {
  override def copy[T](prop: Property[T], value: T): OharaException = {
    val clone = config.snapshot
    prop.set(clone, value)
    new OharaException(clone)
  }

  override protected def extraProperties: Seq[Property[_]] = OharaException.properties

  def stack: String = OharaException.stackProperty.require(config)

  def message: String = OharaException.descriptionProperty.require(config)
}

object OharaException {

  /**
    * a auto-increment number. We don't care about the uuid of OharaRequest.
    */
  private[this] val INDEXER = new AtomicLong(0)

  def apply(exception: Throwable): OharaException = apply(exception.getMessage, exception)

  def apply(description: String, exception: Throwable): OharaException = {
    val config = OharaConfig()
    OharaData.uuidProperty.set(config, INDEXER.getAndIncrement().toString)
    OharaData.nameProperty.set(config, OharaException.getClass.getSimpleName)
    stackProperty.set(config, ExceptionUtils.getStackTrace(exception))
    descriptionProperty.set(config, description)
    new OharaException(config)
  }
  def properties: Seq[Property[_]] = Array(descriptionProperty, stackProperty)
  val descriptionProperty: Property[String] = Property.builder
    .key("ohara-exception-description")
    .alias("description")
    .description("the description of ohara exception")
    .stringProperty
  val stackProperty: Property[String] = Property.builder
    .key("ohara-exception-stack")
    .alias("stack")
    .description("the stack of ohara exception")
    .stringProperty
}
