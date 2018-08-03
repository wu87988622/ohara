package com.island.ohara.data

import com.island.ohara.config.{OharaConfig, OharaJson, OharaProperty}
import org.apache.commons.lang3.exception.ExceptionUtils
import OharaException._

/**
  * Represent a remote exception. It can carray the error stack (in string) and the description of exception.
  * If server encounter something unhandleable error, the OharaExcpeiotn will be created and be a response.
  * Usually, user won't create this object.
  * @param config stores all properties
  */
class OharaException(config: OharaConfig) extends OharaData(config, true) {
  override def copy[T](prop: OharaProperty[T], value: T): OharaException = {
    val clone = config.snapshot
    prop.set(clone, value)
    new OharaException(clone)
  }

  override protected def extraProperties: Seq[OharaProperty[_]] = PROPERTIES

  def stack: String = STACK.require(config)

  def message: String = MESSAGE.require(config)

  def code: String = CODE.require(config)
}

object OharaException {

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
    STACK.set(config, ExceptionUtils.getStackTrace(exception))
    MESSAGE.set(config, message)
    CODE.set(config, exception.getClass.getName)
    new OharaException(config)
  }

  val CODE: OharaProperty[String] =
    OharaProperty.builder.key("code").description("the error code of ohara exception").stringProperty
  val MESSAGE: OharaProperty[String] =
    OharaProperty.builder.key("message").description("the message of ohara exception").stringProperty
  val STACK: OharaProperty[String] =
    OharaProperty.builder.key("stack").description("the stack of ohara exception").stringProperty
  val PROPERTIES: Seq[OharaProperty[_]] = Array(CODE, MESSAGE, STACK)

}
