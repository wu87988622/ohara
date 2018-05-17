package com.island.ohara.config

import java.io.StringReader
import java.util.Properties

import com.typesafe.config.{ConfigFactory, ConfigRenderOptions, ConfigValueFactory}

import scala.collection.mutable

/**
  * Implemented by typesafe config.
  */
private class MapConfig extends OharaConfig {
  private def this(another: Map[String, String]) = {
    this
    another.foreach {
      case (key, value) => set(key, value)
    }
  }
  private[this] val config = new mutable.HashMap[String, String]

  override def get[T](prop: Property[T]): T = config.get(prop.key).map(prop.from(_)).getOrElse(prop.default)

  override def exist(key: String): Boolean = config.contains(key)

  override def iterator: Iterator[(String, String)] = config.iterator

  override def set(key: String, value: String): Option[String] = {
    val previous = config.get(key)
    config.update(key, value)
    previous
  }

  override def toJson: OharaJson = {
    var typeSafe = ConfigFactory.empty()
    foreach {
      case (key, value) => typeSafe = typeSafe.withValue(key, ConfigValueFactory.fromAnyRef(value))
    }
    OharaJson(typeSafe.root().render(ConfigRenderOptions.concise()))
  }

  override def merge(json: OharaJson): OharaConfig = {
    val copy = snapshot
    ConfigFactory
      .parseReader(new StringReader(json.asString))
      .entrySet()
      .forEach(entry => {
        copy.set(entry.getKey, entry.getValue.unwrapped().asInstanceOf[String])
      })
    copy
  }

  override def get(key: String): Option[String] = config.get(key)

  override def toProperties: Properties = {
    val props = new Properties
    this.foreach {
      case (key, value) => props.setProperty(key, value)
    }
    props
  }

  override def load(json: OharaJson): OharaConfig = {
    ConfigFactory
      .parseReader(new StringReader(json.asString))
      .entrySet()
      .forEach(entry => {
        set(entry.getKey, entry.getValue.unwrapped().asInstanceOf[String])
      })
    this
  }
}

private object MapConfig {

  def apply(config: OharaConfig): OharaConfig = {
    val map = new mutable.HashMap[String, String]
    config.foreach{
      case (key, value) => map.put(key, value)
    }
    new MapConfig(map.toMap)
  }

  def apply(props: Properties): MapConfig = {
    val map = new mutable.HashMap[String, String]
    props.forEach((k, v) => map.put(k.asInstanceOf[String], v.asInstanceOf[String]))
    new MapConfig(map.toMap)
  }
  def apply(json: OharaJson): MapConfig = {
    val map = new mutable.HashMap[String, String]
    ConfigFactory
      .parseReader(new StringReader(json.asString))
      .entrySet()
      .forEach(entry => {
        map.put(entry.getKey, entry.getValue.unwrapped().asInstanceOf[String])
      })
    new MapConfig(map.toMap)
  }
}
