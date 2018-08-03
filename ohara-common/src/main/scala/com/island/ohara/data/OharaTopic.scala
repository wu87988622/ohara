package com.island.ohara.data

import com.island.ohara.config.{OharaConfig, OharaJson, OharaProperty}

import OharaTopic._
import OharaData._

/**
  * a pojo to represent the description of ohara topic
  * @param config stores all properties
  */
class OharaTopic(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[OharaProperty[_]] = PROPERTIES

  def numberOfPartitions: Int = NUMBER_OF_PARTITIONS.require(config)

  def numberOfReplications: Short = NUMBER_OF_REPLICATIONS.require(config)
  override def copy[T](prop: OharaProperty[T], value: T): OharaTopic = {
    val clone = config.snapshot
    prop.set(clone, value)
    new OharaTopic(clone)
  }
}

object OharaTopic {

  /**
    * Create the a ohara topic in json format. This helper method is used to sent the schema request to rest server.
    * NOTED: it is used in testing only
    * @param name name
    * @return json
    */
  def json(name: String, numberOfPartitions: Int, numberOfReplications: Short): OharaJson = {
    val config = OharaConfig()
    NAME.set(config, name)
    NUMBER_OF_PARTITIONS.set(config, numberOfPartitions)
    NUMBER_OF_REPLICATIONS.set(config, numberOfReplications)
    config.toJson
  }

  /**
    * create a OharaTopic with specified config
    * @param config config
    * @return a new OharaTopic
    */
  def apply(json: OharaJson) = new OharaTopic(OharaConfig(json))

  /**
    * create a OharaTopic with specified config
    * @param config config
    * @return a new OharaTopic
    */
  def apply(config: OharaConfig) = new OharaTopic(config)

  def apply(uuid: String, otherOptions: OharaJson): OharaTopic = {
    val oharaConfig = OharaConfig(otherOptions)
    UUID.set(oharaConfig, uuid)
    new OharaTopic(oharaConfig)
  }

  /**
    * create an new OharaTopic with specified arguments
    * @param uuid uuid
    * @param name target name
    * @param numberOfPartitions the number of partition
    * @param numberOfReplications the number of replication
    * @return an new OharaTopic
    */
  def apply(uuid: String, name: String, numberOfPartitions: Int, numberOfReplications: Short): OharaTopic = {
    val oharaConfig = OharaConfig()
    UUID.set(oharaConfig, uuid)
    NAME.set(oharaConfig, name)
    NUMBER_OF_PARTITIONS.set(oharaConfig, numberOfPartitions)
    NUMBER_OF_REPLICATIONS.set(oharaConfig, numberOfReplications)
    new OharaTopic(oharaConfig)
  }

  val NUMBER_OF_PARTITIONS: OharaProperty[Int] =
    OharaProperty.builder.key("numberOfPartitions").description("the number of partition of ohara topic").intProperty(1)
  val NUMBER_OF_REPLICATIONS: OharaProperty[Short] = OharaProperty.builder
    .key("numberOfReplications")
    .description("the number of replication of ohara topic")
    .shortProperty(3)
  val PROPERTIES: Seq[OharaProperty[_]] = Array(NUMBER_OF_PARTITIONS, NUMBER_OF_REPLICATIONS)
}
