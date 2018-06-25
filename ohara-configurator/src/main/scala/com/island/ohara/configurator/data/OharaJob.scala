package com.island.ohara.configurator.data

import com.island.ohara.config.{OharaConfig, OharaProperty}

/**
  * a pojo to represent the description of ohara job
  * @param config stores all properties
  */
class OharaJob(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[OharaProperty[_]] = OharaJob.properties

  def status: JobStatus = OharaJob.status.require(config)
  def rules: Map[String, Seq[String]] = OharaJob.rules.require(config)

  override def copy[T](prop: OharaProperty[T], value: T): OharaJob = {
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
    OharaData.uuid.set(oharaConfig, uuid)
    OharaData.name.set(oharaConfig, name)
    OharaJob.status.set(oharaConfig, status)
    OharaJob.rules.set(oharaConfig, rules)
    new OharaJob(oharaConfig)
  }
  def properties: Seq[OharaProperty[_]] = Array(status, rules)
  val status: OharaProperty[JobStatus] = OharaProperty.builder
    .key("status")
    .description("the status of ohara job")
    .property(JobStatus.of(_), _.name, JobStatus.STOP)
  val rules: OharaProperty[Map[String, Seq[String]]] = OharaProperty.builder
    .key("rules")
    .description("the rules of ohara job")
    .mapProperty(_.replace(" ", "").split(SEPARATE_CHAR), _.mkString(SEPARATE_CHAR))

  val SEPARATE_CHAR = ","
}
