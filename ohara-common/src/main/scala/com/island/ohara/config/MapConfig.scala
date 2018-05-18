package com.island.ohara.config

import java.io.StringReader
import java.util.Properties

import com.typesafe.config.{ConfigFactory, ConfigRenderOptions, ConfigValueFactory}
import com.typesafe.scalalogging.Logger

import scala.collection.mutable

/**
  * Implemented by typesafe config.
  */
private class MapConfig(another: Map[String, Either[String, Map[String, String]]] = null) extends OharaConfig {
  private[this] val logger = Logger(getClass.getName)
  private[this] val config = new mutable.HashMap[String, Either[String, Map[String, String]]]()
  if (another != null) {
    config ++= another
    logger.info("Succeed to initialize MapConfig by loading another config")
  }

  override def get[T](prop: Property[T]): T = config
    .get(prop.key)
    .map {
      case Left(s)  => prop.from(s)
      case Right(s) => prop.from(s)
    }
    .getOrElse(prop.default)

  override def exist(key: String): Boolean = config.contains(key)

  override def iterator: Iterator[(String, Either[String, Map[String, String]])] = config.iterator

  override def set(key: String, value: String): Option[Either[String, Map[String, String]]] = {
    val previous = config.get(key)
    config.update(key, Left(value))
    previous
  }

  override def toJson: OharaJson = {
    import scala.collection.JavaConverters._
    var typeSafe = ConfigFactory.empty()
    foreach {
      case (key, value) =>
        value match {
          case Left(s)  => typeSafe = typeSafe.withValue(key, ConfigValueFactory.fromAnyRef(s))
          case Right(s) => typeSafe = typeSafe.withValue(key, ConfigValueFactory.fromAnyRef(s.asJava))
        }
    }
    OharaJson(typeSafe.root().render(ConfigRenderOptions.concise()))
  }

  override def get(key: String): Option[Either[String, Map[String, String]]] = config.get(key)

  override def toProperties: Properties = {
    val props = new Properties
    this.foreach {
      case (key, value) =>
        value match {
          case Left(s)  => props.setProperty(key, s)
          case Right(s) => props.put(key, s)
        }
    }
    props
  }

  override def set(key: String, value: Map[String, String]): Option[Either[String, Map[String, String]]] =
    config.put(key, Right(value))

}

private object MapConfig {

  def apply(config: OharaConfig): OharaConfig = {
    val map = new mutable.HashMap[String, Either[String, Map[String, String]]]
    config.foreach {
      case (key, value) => map.put(key, value)
    }
    new MapConfig(map.toMap)
  }

  def apply(props: Properties): MapConfig = {
    val map = new mutable.HashMap[String, Either[String, Map[String, String]]]
    props.forEach((k, v) =>
      v match {
        case s: String              => map.put(k.asInstanceOf[String], Left(s))
        case s: Map[String, String] => map.put(k.asInstanceOf[String], Right(s))
        case s: Object =>
          throw new UnsupportedClassVersionError(
            s"Only accept the string or Map<String, String>, actual:${s.getClass.getName}")
    })
    new MapConfig(map.toMap)
  }
  def apply(json: OharaJson): MapConfig = {
    import scala.collection.JavaConverters._
    val map = new mutable.HashMap[String, Either[String, Map[String, String]]]
    ConfigFactory
      .parseReader(new StringReader(json.asString))
      // convert the config to tree in order to get the map type
      .root
      .entrySet()
      .forEach(entry =>
        entry.getValue.unwrapped() match {
          case s: String                        => map.put(entry.getKey, Left(s))
          case s: java.util.Map[String, String] => map.put(entry.getKey, Right(s.asScala.toMap))
      })
    new MapConfig(map.toMap)
  }
}
