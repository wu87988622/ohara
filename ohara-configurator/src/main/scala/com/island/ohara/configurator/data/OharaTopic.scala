package com.island.ohara.configurator.data

import com.island.ohara.config.{OharaConfig, OharaProperty}

/**
  * a pojo to represent the description of ohara topic
  * @param config stores all properties
  */
class OharaTopic(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[OharaProperty[_]] = OharaTopic.properties

  def numberOfPartition: Int = OharaTopic.partitionNumber.require(config)

  def numberOfReplication: Int = OharaTopic.replicationNumber.require(config)
  override def copy[T](prop: OharaProperty[T], value: T): OharaTopic = {
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
    OharaData.uuid.set(oharaConfig, uuid)
    OharaData.name.set(oharaConfig, name)
    OharaTopic.partitionNumber.set(oharaConfig, partitionNumber)
    OharaTopic.replicationNumber.set(oharaConfig, replicationNumber)
    new OharaTopic(oharaConfig)
  }

  def properties: Seq[OharaProperty[_]] = Array(partitionNumber, replicationNumber)
  val partitionNumber: OharaProperty[Int] =
    OharaProperty.builder.key("partitionNumber").description("the number of partition of ohara topic").intProperty(1)
  val replicationNumber: OharaProperty[Int] = OharaProperty.builder
    .key("replicationNumber")
    .description("the number of replication of ohara topic")
    .intProperty(3)
}
