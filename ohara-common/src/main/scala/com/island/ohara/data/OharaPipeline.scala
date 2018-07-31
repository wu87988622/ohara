package com.island.ohara.data

import com.island.ohara.config.{OharaConfig, OharaProperty}

/**
  * a pojo to represent the description of ohara pipeline
  * @param config stores all properties
  */
class OharaPipeline(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[OharaProperty[_]] = OharaPipeline.properties

  def status: PipelineStatus = OharaPipeline.status.require(config)
  def rules: Map[String, Seq[String]] = OharaPipeline.rules.require(config)

  override def copy[T](prop: OharaProperty[T], value: T): OharaPipeline = {
    val clone = config.snapshot
    prop.set(clone, value)
    new OharaPipeline(clone)
  }
}

object OharaPipeline {

  /**
    * create a OharaPipeline with specified config
    * @param config config
    * @return a new OharaPipeline
    */
  def apply(config: OharaConfig) = new OharaPipeline(config)

  /**
    * create a OharaPipeline with specified arguments
    * @param uuid uuid
    * @param name pipeline name
    * @param status pipeline status
    * @param rules pipeline rules
    * @return a new OharaPipeline
    */
  def apply(uuid: String, name: String, status: PipelineStatus, rules: Map[String, Seq[String]]): OharaPipeline = {
    val oharaConfig = OharaConfig()
    OharaData.uuid.set(oharaConfig, uuid)
    OharaData.name.set(oharaConfig, name)
    OharaPipeline.status.set(oharaConfig, status)
    OharaPipeline.rules.set(oharaConfig, rules)
    new OharaPipeline(oharaConfig)
  }
  def properties: Seq[OharaProperty[_]] = Array(status, rules)
  val status: OharaProperty[PipelineStatus] = OharaProperty.builder
    .key("status")
    .description("the status of ohara pipeline")
    .property(PipelineStatus.of(_), _.name, PipelineStatus.STOP)
  val rules: OharaProperty[Map[String, Seq[String]]] = OharaProperty.builder
    .key("rules")
    .description("the rules of ohara pipeline")
    .mapProperty(_.replace(" ", "").split(SEPARATE_CHAR), _.mkString(SEPARATE_CHAR))

  val SEPARATE_CHAR = ","
}
