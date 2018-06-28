package com.island.ohara.configurator

import java.util.concurrent.TimeUnit

import com.island.ohara.configurator.data.{OharaData, OharaDataSerializer}
import com.island.ohara.configurator.store.{MemStore, Store}
import com.island.ohara.serialization.StringSerializer

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

  /**
    * set the in-memory store. Used in testing when there is no kafka.
    * @return this builder
    */
  def inMemoryStore(): ConfiguratorBuilder = {
    store = Some(new MemStore[String, OharaData](StringSerializer, OharaDataSerializer))
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

object ConfiguratorBuilder {

  /**
    * Running a standalone configurator.
    * NOTED: this main is exposed to build.gradle. If you want to move the main out of this class, please update the
    * build.gradle also.
    * @param args the first element is hostname and the second one is port
    */
  def main(args: Array[String]): Unit = {
    // TODO: make the parse more friendly
    val configurator = args.length match {
      case 2 =>
        Configurator.builder.hostname(args(0)).port(args(1).toInt).inMemoryStore().build()
      case 1 => throw new UnsupportedOperationException("The configurator in production hasn't been completed")
      case _ => throw new IllegalArgumentException("[Usage] <hostname> <port")
    }
    hasRunningConfigurator = true
    try {
      println(
        s"start a standalone configurator built on hostname:${configurator.hostname} and port:${configurator.port}")
      println("enter ctrl+c to terminate the configurator")
      while (!closeRunningConfigurator) {
        TimeUnit.SECONDS.sleep(2)
        println(s"Current data size:${configurator.size}")
      }
    } catch {
      case _: InterruptedException => println("prepare to die")
    } finally configurator.close()
  }

  /**
    * visible for testing.
    */
  @volatile private[configurator] var hasRunningConfigurator = false

  /**
    * visible for testing.
    */
  @volatile private[configurator] var closeRunningConfigurator = false
}
