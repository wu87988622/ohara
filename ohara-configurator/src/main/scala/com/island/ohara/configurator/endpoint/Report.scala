package com.island.ohara.configurator.endpoint

import com.island.ohara.config.{OharaConfig, OharaProperty}
import com.island.ohara.configurator.endpoint.Report._
import com.island.ohara.data.OharaData

/**
  * an inner object used to carry the test result from connectors
  * @param config stores all properties
  */
class Report(config: OharaConfig) extends OharaData(config, true) {

  override def copy[T](prop: OharaProperty[T], value: T): Report = {
    val clone = config.snapshot
    prop.set(clone, value)
    new Report(clone)
  }

  def hostname: String = HOST_NAME.require(config)
  def message: String = MESSAGE.require(config)
  def pass: Boolean = PASS.require(config)
  override protected def extraProperties: Seq[OharaProperty[_]] = PROPERTIES
}

object Report {

  def apply(hostname: String, message: String): Report = {
    val config = OharaConfig()
    HOST_NAME.set(config, hostname)
    MESSAGE.set(config, message)
    PASS.set(config, true)
    new Report(config)
  }

  def apply(hostname: String, exception: Throwable): Report = {
    val config = OharaConfig()
    HOST_NAME.set(config, hostname)
    MESSAGE.set(config, exception.getMessage)
    PASS.set(config, false)
    new Report(config)
  }

  val HOST_NAME: OharaProperty[String] =
    OharaProperty.builder.key("hostname").description("the hostname of worker").stringProperty

  val MESSAGE: OharaProperty[String] =
    OharaProperty.builder.key("message").description("the message of report").stringProperty

  val PASS: OharaProperty[Boolean] =
    OharaProperty.builder.key("pass").description("the status of report").booleanProperty

  val PROPERTIES: Seq[OharaProperty[_]] = Array(HOST_NAME, MESSAGE, PASS)
}
