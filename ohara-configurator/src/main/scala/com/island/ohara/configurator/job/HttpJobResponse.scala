package com.island.ohara.configurator.job

import com.island.ohara.config.{OharaConfig, OharaProperty, UuidUtil}
import com.island.ohara.configurator.data.OharaData

class HttpJobResponse(oharaConfig: OharaConfig) extends OharaData(oharaConfig) {

  override def copy[T](prop: OharaProperty[T], value: T): HttpJobResponse = {
    val clone = oharaConfig.snapshot
    prop.set(clone, value)
    new HttpJobResponse(clone)
  }

  def status: Status = HttpJobResponse.status.require(oharaConfig)
  def config: Map[String, String] = HttpJobResponse.config.require(oharaConfig)

  override protected def extraProperties: Seq[OharaProperty[_]] = HttpJobResponse.properties
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
    OharaData.uuid.set(oharaConfig, uuid)
    OharaData.name.set(oharaConfig, name)
    HttpJobResponse.config.set(oharaConfig, config)
    HttpJobResponse.status.set(oharaConfig, status)
    new HttpJobResponse(oharaConfig)
  }

  def properties: Seq[OharaProperty[_]] = Array(config, status)

  val status: OharaProperty[Status] =
    OharaProperty.builder.key("status").description("the status of http job response").property(Status.of(_), _.name)
  val config: OharaProperty[Map[String, String]] =
    OharaProperty.builder.key("config").description("the config of http job response").mapProperty
}
