package com.island.ohara.configurator.call

import com.island.ohara.config.{OharaConfig, OharaProperty}
import com.island.ohara.configurator.data.OharaData

/**
  * a internal-purposed ohara data. see CallQueueServerImpl and CallQueueClientImpl for more details.
  * @param config stores all properties
  */
private class OharaResponse(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[OharaProperty[_]] = OharaResponse.properties

  def requestId: String = OharaResponse.requestIdProperty.require(config)
  override def copy[T](prop: OharaProperty[T], value: T): OharaResponse = {
    val clone = config.snapshot
    prop.set(clone, value)
    new OharaResponse(clone)
  }
}

private object OharaResponse {

  /**
    * create a OharaJob with specified config
    * @param reqId id of request
    * @return a new OharaJob
    */
  def apply(uuid: String, reqId: String): OharaResponse = {
    val config = OharaConfig()
    OharaData.uuidProperty.set(config, uuid)
    OharaData.nameProperty.set(config, OharaResponse.getClass.getSimpleName)
    requestIdProperty.set(config, reqId)
    new OharaResponse(config)
  }

  def properties: Seq[OharaProperty[_]] = Array(requestIdProperty)
  val requestIdProperty: OharaProperty[String] = OharaProperty.builder
    .key("ohara-response-request-id")
    .alias("request-id")
    .description("the uuid against the ohara response")
    .stringProperty
}
