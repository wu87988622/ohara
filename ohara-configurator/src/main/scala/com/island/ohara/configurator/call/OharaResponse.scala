package com.island.ohara.configurator.call

import com.island.ohara.config.{OharaConfig, OharaProperty}
import com.island.ohara.data.OharaData

/**
  * a internal-purposed ohara data. see CallQueueServerImpl and CallQueueClientImpl for more details.
  * @param config stores all properties
  */
private class OharaResponse(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[OharaProperty[_]] = OharaResponse.properties

  def requestId: String = OharaResponse.requestId.require(config)
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
    OharaData.uuid.set(config, uuid)
    OharaData.name.set(config, OharaResponse.getClass.getSimpleName)
    OharaResponse.requestId.set(config, reqId)
    new OharaResponse(config)
  }

  def properties: Seq[OharaProperty[_]] = Array(requestId)
  val requestId: OharaProperty[String] =
    OharaProperty.builder.key("requestId").description("the uuid against the ohara response").stringProperty
}
