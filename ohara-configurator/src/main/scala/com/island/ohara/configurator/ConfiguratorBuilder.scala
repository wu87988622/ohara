package com.island.ohara.configurator

import com.island.ohara.configurator.data.OharaData
import com.island.ohara.configurator.store.Store

import scala.concurrent.duration.Duration

class ConfiguratorBuilder {
  private[this] var uuidGenerator: Option[() => String] = Some(Configurator.DEFAULT_UUID_GENERATOR)
  private[this] var hostname: Option[String] = None
  private[this] var port: Option[Int] = None
  private[this] var store: Option[Store[String, OharaData]] = None
  private[this] var initializationTimeout: Option[Duration] = Some(Configurator.DEFAULT_INITIALIZATION_TIMEOUT)
  private[this] var terminationTimeout: Option[Duration] = Some(Configurator.DEFAULT_TERMINATION_TIMEOUT)

  /**
    * set a specified uuid generator.
    * @param generator uuid generator
    * @return this builder
    */
  def uuidGenerator(generator: () => String): ConfiguratorBuilder = {
    uuidGenerator = Some(generator)
    this
  }

  /**
    * set a specified hostname
    * @param _hostname used to build the rest server
    * @return this builder
    */
  def hostname(_hostname: String): ConfiguratorBuilder = {
    hostname = Some(_hostname)
    this
  }

  /**
    * set a specified port
    * @param _port used to build the rest server
    * @return this builder
    */
  def port(_port: Int): ConfiguratorBuilder = {
    port = Some(_port)
    this
  }

  /**
    * set a specified store used to maintain the ohara data.
    * NOTED: Configurator has responsibility to release this store.
    * @param _store used to maintain the ohara data.
    * @return this builder
    */
  def store(_store: Store[String, OharaData]): ConfiguratorBuilder = {
    store = Some(_store)
    this
  }

  def terminationTimeout(_terminationTimeout: Duration): ConfiguratorBuilder = {
    terminationTimeout = Some(_terminationTimeout)
    this
  }

  def initializationTimeout(_initializationTimeout: Duration): ConfiguratorBuilder = {
    initializationTimeout = Some(_initializationTimeout)
    this
  }

  def build(): Configurator = new ConfiguratorImpl(uuidGenerator.get,
                                                   hostname.get,
                                                   port.get,
                                                   store.get,
                                                   initializationTimeout.get,
                                                   terminationTimeout.get)
}
