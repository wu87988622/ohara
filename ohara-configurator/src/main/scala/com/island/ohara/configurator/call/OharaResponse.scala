package com.island.ohara.configurator.call

import com.island.ohara.config.{OharaConfig, OharaProperty}
import com.island.ohara.data.OharaData

import OharaResponse._
import OharaData._

/**
  * a internal-purposed ohara data. see CallQueueServerImpl and CallQueueClientImpl for more details.
  * @param config stores all properties
  */
private class OharaResponse(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[OharaProperty[_]] = PROPERTIES

  def requestId: String = REQUEST_ID.require(config)
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
    UUID.set(config, uuid)
    NAME.set(config, OharaResponse.getClass.getSimpleName)
    REQUEST_ID.set(config, reqId)
    new OharaResponse(config)
  }

  val REQUEST_ID: OharaProperty[String] =
    OharaProperty.builder.key("requestId").description("the uuid against the ohara response").stringProperty
  val PROPERTIES: Seq[OharaProperty[_]] = Array(REQUEST_ID)

}
