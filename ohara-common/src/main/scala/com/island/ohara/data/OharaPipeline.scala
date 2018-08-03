package com.island.ohara.data

import com.island.ohara.config.{OharaConfig, OharaProperty}

import OharaPipeline._
import OharaData._

/**
  * a pojo to represent the description of ohara pipeline
  * @param config stores all properties
  */
class OharaPipeline(config: OharaConfig) extends OharaData(config) {

  override protected def extraProperties: Seq[OharaProperty[_]] = PROPERTIES

  def status: PipelineStatus = STATUS.require(config)
  def rules: Map[String, Seq[String]] = RULES.require(config)

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
    UUID.set(oharaConfig, uuid)
    NAME.set(oharaConfig, name)
    STATUS.set(oharaConfig, status)
    RULES.set(oharaConfig, rules)
    new OharaPipeline(oharaConfig)
  }
  val STATUS: OharaProperty[PipelineStatus] = OharaProperty.builder
    .key("status")
    .description("the status of ohara pipeline")
    .property(PipelineStatus.of(_), _.name, PipelineStatus.STOP)
  val RULES: OharaProperty[Map[String, Seq[String]]] = OharaProperty.builder
    .key("rules")
    .description("the rules of ohara pipeline")
    .mapProperty(_.replace(" ", "").split(SEPARATE_CHAR), _.mkString(SEPARATE_CHAR))
  val PROPERTIES: Seq[OharaProperty[_]] = Array(STATUS, RULES)
  val SEPARATE_CHAR = ","
}
