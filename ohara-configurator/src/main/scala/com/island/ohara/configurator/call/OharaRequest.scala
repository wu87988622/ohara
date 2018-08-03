package com.island.ohara.configurator.call

import com.island.ohara.config.{OharaConfig, OharaProperty}
import com.island.ohara.data.OharaData

import scala.concurrent.duration.Duration

import OharaRequest._
import OharaData._

/**
  * a internal-purposed ohara data. see CallQueueServerImpl and CallQueueClientImpl for more details.
  * @param config stores all properties
  */
private class OharaRequest(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[OharaProperty[_]] = PROPERTIES

  def lease: Long = LEASE.require(config)

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
    UUID.set(config, uuid)
    NAME.set(config, OharaRequest.getClass.getSimpleName)
    LEASE.set(config, lease)
    new OharaRequest(config)
  }

  val LEASE: OharaProperty[Long] = OharaProperty.builder
    .key("requestLease")
    .description("the lease of the ohara request")
    .longProperty(CallQueue.DEFAULT_EXPIRATION_TIME.toMillis)
  val PROPERTIES: Seq[OharaProperty[_]] = Array[OharaProperty[_]](LEASE)

}
