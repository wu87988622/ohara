package com.island.ohara.configurator.data

import com.island.ohara.config.{OharaConfig, Property}

/**
  * a pojo to represent the description of ohara topic
  * @param config stores all properties
  */
class OharaTopic(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[Property[_]] = OharaTopic.properties

  def numberOfPartition: Int = OharaTopic.partitionProperty.require(config)

  def numberOfReplication: Int = OharaTopic.replicationProperty.require(config)
  override def copy[T](prop: Property[T], value: T): OharaTopic = {
    val clone = config.snapshot
    prop.set(clone, value)
    new OharaTopic(clone)
  }
}

object OharaTopic {

  /**
    * create a OharaTopic with specified config
    * @param config config
    * @return a new OharaTopic
    */
  def apply(config: OharaConfig) = new OharaTopic(config)

  /**
    * create an new OharaTopic with specified arguments
    * @param uuid uuid
    * @param name target name
    * @param partitionNumber the number of partition
    * @param replicationNumber the number of replication
    * @return an new OharaTopic
    */
  def apply(uuid: String, name: String, partitionNumber: Int, replicationNumber: Int): OharaTopic = {
    val oharaConfig = OharaConfig()
    OharaData.uuidProperty.set(oharaConfig, uuid)
    OharaData.nameProperty.set(oharaConfig, name)
    partitionProperty.set(oharaConfig, partitionNumber)
    replicationProperty.set(oharaConfig, replicationNumber)
    new OharaTopic(oharaConfig)
  }

  def properties: Seq[Property[_]] = Array(partitionProperty, replicationProperty)
  val partitionProperty: Property[Int] = Property.builder
    .key("ohara-topic-partitions")
    .alias("partitionNumber")
    .description("the number of partition of ohara topic")
    .intProperty(1)
  val replicationProperty: Property[Int] = Property.builder
    .key("ohara-topic-replications")
    .alias("replicationNumber")
    .description("the number of replication of ohara topic")
    .intProperty(3)
}
