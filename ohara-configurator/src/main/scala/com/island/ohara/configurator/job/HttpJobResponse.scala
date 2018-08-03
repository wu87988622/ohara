package com.island.ohara.configurator.job

import com.island.ohara.config.{OharaConfig, OharaProperty, UuidUtil}
import com.island.ohara.data.OharaData
import HttpJobResponse._
import OharaData._
class HttpJobResponse(oharaConfig: OharaConfig) extends OharaData(oharaConfig) {

  override def copy[T](prop: OharaProperty[T], value: T): HttpJobResponse = {
    val clone = oharaConfig.snapshot
    prop.set(clone, value)
    new HttpJobResponse(clone)
  }

  def status: Status = STATUS.require(oharaConfig)
  def config: Map[String, String] = CONFIG.require(oharaConfig)

  override protected def extraProperties: Seq[OharaProperty[_]] = PROPERTIES
}

object HttpJobResponse {

  /**
    * new a HttpJobResponse with an arbitrary uuid and the simple name
    * @param status status
    * @param config config
    * @return an new HttpJobResponse
    */
  def apply(status: Status, config: Map[String, String]): HttpJobResponse =
    apply(UuidUtil.uuid(), classOf[HttpJobResponse].getSimpleName, status, config)

  /**
    * new a HttpJobResponse
    * @param uuid uuid
    * @param name name
    * @param status status
    * @param config config
    * @return an new HttpJobResponse
    */
  def apply(uuid: String, name: String, status: Status, config: Map[String, String]): HttpJobResponse = {
    val oharaConfig = OharaConfig()
    UUID.set(oharaConfig, uuid)
    NAME.set(oharaConfig, name)
    CONFIG.set(oharaConfig, config)
    STATUS.set(oharaConfig, status)
    new HttpJobResponse(oharaConfig)
  }

  def PROPERTIES: Seq[OharaProperty[_]] = Array(CONFIG, STATUS)

  val STATUS: OharaProperty[Status] =
    OharaProperty.builder.key("status").description("the status of http job response").property(Status.of(_), _.name)
  val CONFIG: OharaProperty[Map[String, String]] =
    OharaProperty.builder.key("config").description("the config of http job response").mapProperty
}
