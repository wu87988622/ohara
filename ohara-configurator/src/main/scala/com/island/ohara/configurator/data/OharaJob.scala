package com.island.ohara.configurator.data

import com.island.ohara.config.{OharaConfig, Property}

/**
  * a pojo to represent the description of ohara job
  * @param config stores all properties
  */
class OharaJob(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[Property[_]] = OharaJob.properties

  def status: JobStatus = OharaJob.statusProperty.require(config)
  def rules: Map[String, Seq[String]] = OharaJob.rulesProperty.require(config)

  override def copy[T](prop: Property[T], value: T): OharaJob = {
    val clone = config.snapshot
    prop.set(clone, value)
    new OharaJob(clone)
  }
}

object OharaJob {

  /**
    * create a OharaJob with specified config
    * @param config config
    * @return a new OharaJob
    */
  def apply(config: OharaConfig) = new OharaJob(config)

  /**
    * create a OharaJob with specified arguments
    * @param uuid uuid
    * @param name job name
    * @param status job status
    * @param rules job rules
    * @return a new OharaJob
    */
  def apply(uuid: String, name: String, status: JobStatus, rules: Map[String, Seq[String]]): OharaJob = {
    val oharaConfig = OharaConfig()
    OharaData.uuidProperty.set(oharaConfig, uuid)
    OharaData.nameProperty.set(oharaConfig, name)
    statusProperty.set(oharaConfig, status)
    rulesProperty.set(oharaConfig, rules)
    new OharaJob(oharaConfig)
  }
  def properties: Seq[Property[_]] = Array(statusProperty, rulesProperty)
  val statusProperty: Property[JobStatus] = Property.builder
    .key("ohara-job-status")
    .alias("status")
    .description("the status of ohara job")
    .property(JobStatus.of(_), _.name, JobStatus.STOP)
  val rulesProperty: Property[Map[String, Seq[String]]] = Property.builder
    .key("ohara-job-rules")
    .alias("rules")
    .description("the rules of ohara job")
    .mapProperty(_.replace(" ", "").split(SEPARATE_CHAR), _.mkString(SEPARATE_CHAR))

  val SEPARATE_CHAR = ","
}
