package com.island.ohara.configurator.call

import com.island.ohara.config.{OharaConfig, OharaProperty}
import com.island.ohara.data.OharaData

import scala.concurrent.duration.Duration

/**
  * a internal-purposed ohara data. see CallQueueServerImpl and CallQueueClientImpl for more details.
  * @param config stores all properties
  */
private class OharaRequest(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[OharaProperty[_]] = OharaRequest.properties

  def lease: Long = OharaRequest.lease.require(config)

  override def copy[T](prop: OharaProperty[T], value: T): OharaRequest = {
    val clone = config.snapshot
    prop.set(clone, value)
    new OharaRequest(clone)
  }
}

private object OharaRequest {

  /**
    * create a OharaJob with specified config
    * @return a new OharaJob
    */
  def apply(uuid: String, lease: Duration): OharaRequest = apply(uuid, lease.toMillis)

  /**
    * create a OharaJob with specified config
    * @return a new OharaJob
    */
  def apply(uuid: String, lease: Long): OharaRequest = {
    val config = OharaConfig()
    OharaData.uuid.set(config, uuid)
    OharaData.name.set(config, OharaRequest.getClass.getSimpleName)
    OharaRequest.lease.set(config, lease)
    new OharaRequest(config)
  }

  def properties: Seq[OharaProperty[_]] = Array[OharaProperty[_]](lease)
  val lease: OharaProperty[Long] = OharaProperty.builder
    .key("requestLease")
    .description("the lease of the ohara request")
    .longProperty(CallQueue.DEFAULT_EXPIRATION_TIME.toMillis)

}
