package com.island.ohara.configurator

import com.island.ohara.config.UuidUtil
import com.island.ohara.configurator.data.{OharaData, OharaSchema}
import com.island.ohara.configurator.store.Store
import com.island.ohara.io.CloseOnce

import scala.concurrent.duration.{Duration, _}

/**
  * Configurator works for controlling the jobs running in Ohara cluster. Configurator hosts a rest server in order
  * to accept the rest request. This way help us to run the Configurator and its user in different process/node.
  *
  * Configurator stores all Ohara data in Ohara Store. Currently the store impl is based on kafka topic.
  * see TopicStore for more details
  */
trait Configurator extends Iterable[OharaData] with CloseOnce {

  /**
    * the hostname of rest server
    */
  val hostname: String

  /**
    * the port of rest server
    */
  val port: Int

  val store: Store[String, OharaData]

  /**
    * @return iteration of schemas managed by configurator
    */
  def schemas: Iterator[OharaSchema]
}

object Configurator {
  def builder = new ConfiguratorBuilder()

  val DEFAULT_UUID_GENERATOR: () => String = () => UuidUtil.uuid()

  val VERSION = "v0"
  val SCHEMA_PATH = "schemas"
  val DEFAULT_INITIALIZATION_TIMEOUT: Duration = 10 seconds
  val DEFAULT_TERMINATION_TIMEOUT: Duration = 10 seconds
  val TOPIC_PATH = "topics"
}
