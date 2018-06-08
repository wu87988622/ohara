package com.island.ohara.configurator.job

import com.island.ohara.config.{OharaConfig, Property, UuidUtil}
import com.island.ohara.configurator.data.OharaData

class HttpJobResponse(oharaConfig: OharaConfig) extends OharaData(oharaConfig) {

  override def copy[T](prop: Property[T], value: T): HttpJobResponse = {
    val clone = oharaConfig.snapshot
    prop.set(clone, value)
    new HttpJobResponse(clone)
  }

  def status: Status = HttpJobResponse.statusProperty.require(oharaConfig)
  def config: Map[String, String] = HttpJobResponse.configProperty.require(oharaConfig)

  override protected def extraProperties: Seq[Property[_]] = HttpJobResponse.properties
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
    OharaData.uuidProperty.set(oharaConfig, uuid)
    OharaData.nameProperty.set(oharaConfig, name)
    configProperty.set(oharaConfig, config)
    statusProperty.set(oharaConfig, status)
    new HttpJobResponse(oharaConfig)
  }

  def properties: Seq[Property[_]] = Array(configProperty, statusProperty)

  val statusProperty: Property[Status] =
    Property.builder
      .key("http-job-response-status")
      .alias("status")
      .description("the status of http job response")
      .property(Status.of(_), _.name)
  val configProperty: Property[Map[String, String]] =
    Property.builder
      .key("http-job-response-config")
      .alias("config")
      .description("the config of http job response")
      .mapProperty
}
