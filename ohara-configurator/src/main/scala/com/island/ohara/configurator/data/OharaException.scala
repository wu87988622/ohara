package com.island.ohara.configurator.data

import java.util.concurrent.atomic.AtomicLong

import com.island.ohara.config.{OharaConfig, OharaJson, OharaProperty}
import org.apache.commons.lang3.exception.ExceptionUtils

/**
  * Represent a remote exception. It can carray the error stack (in string) and the description of exception.
  * If server encounter something unhandleable error, the OharaExcpeiotn will be created and be a response.
  * Usually, user won't create this object.
  * @param config stores all properties
  */
class OharaException(config: OharaConfig) extends OharaData(config) {
  override def copy[T](prop: OharaProperty[T], value: T): OharaException = {
    val clone = config.snapshot
    prop.set(clone, value)
    new OharaException(clone)
  }

  override protected def extraProperties: Seq[OharaProperty[_]] = OharaException.properties

  def stack: String = OharaException.stack.require(config)

  def message: String = OharaException.message.require(config)

  def code: String = OharaException.code.require(config)
}

object OharaException {

  /**
    * a auto-increment number. We don't care about the uuid of OharaRequest.
    */
  private[this] val INDEXER = new AtomicLong(0)

  /**
    * create a OharaSchema with specified config
    * @param json config in json format
    * @return a new OharaSchema
    */
  def apply(json: OharaJson): OharaException = apply(OharaConfig(json))

  /**
    * create a OharaSchema with specified config
    * @param config config
    * @return a new OharaSchema
    */
  def apply(config: OharaConfig): OharaException = new OharaException(config)

  def apply(exception: Throwable): OharaException =
    apply(if (exception.getMessage == null) "None" else exception.getMessage, exception)

  def apply(message: String, exception: Throwable): OharaException = {
    val config = OharaConfig()
    OharaData.uuid.set(config, INDEXER.getAndIncrement().toString)
    OharaData.name.set(config, OharaException.getClass.getSimpleName)
    OharaException.stack.set(config, ExceptionUtils.getStackTrace(exception))
    OharaException.message.set(config, message)
    OharaException.code.set(config, exception.getClass.getName)
    new OharaException(config)
  }
  def properties: Seq[OharaProperty[_]] = Array(code, message, stack)
  val code: OharaProperty[String] =
    OharaProperty.builder.key("code").description("the error code of ohara exception").stringProperty
  val message: OharaProperty[String] =
    OharaProperty.builder.key("message").description("the message of ohara exception").stringProperty
  val stack: OharaProperty[String] =
    OharaProperty.builder.key("stack").description("the stack of ohara exception").stringProperty
}
