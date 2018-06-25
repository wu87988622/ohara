package com.island.ohara.config

import java.io.StringReader
import java.util.{Objects, Properties}

import com.typesafe.config.{ConfigException, ConfigFactory, ConfigRenderOptions, ConfigValueFactory}
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

  override def exist(key: String): Boolean = config.contains(key)

  override def iterator: Iterator[(String, Either[String, Map[String, String]])] = config.iterator

  override def set(key: String, value: String): Option[Either[String, Map[String, String]]] = {
    val previous = config.get(Objects.requireNonNull(key))
    config.update(key, Left(Objects.requireNonNull(value)))
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

  override def get(key: String): Option[Either[String, Map[String, String]]] = config.get(Objects.requireNonNull(key))

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
    config.put(Objects.requireNonNull(key), Right(Objects.requireNonNull(value)))

  override def toPlainMap: Map[String, String] = config
    .filter {
      case (_, v) => v.isLeft
    }
    .map {
      case (k, v) => (k, v.left.get)
    }
    .toMap
}

private object MapConfig {

  def apply(input: Map[String, Any]): MapConfig = {
    val map = new mutable.HashMap[String, Either[String, Map[String, String]]]
    input.foreach {
      case (k, v) =>
        v match {
          case s: String              => map.put(k, Left(s))
          case s: Map[String, String] => map.put(k, Right(s))
          case _ =>
            throw new UnsupportedClassVersionError(
              s"Only accept the string or Map<String, String>, actual:${v.getClass.getName}")
        }
    }
    new MapConfig(map.toMap)
  }

  def apply(config: OharaConfig): MapConfig = {
    val map = new mutable.HashMap[String, Either[String, Map[String, String]]]
    config.foreach {
      case (key, value) => map.put(key, value)
    }
    new MapConfig(map.toMap)
  }

  def apply(props: Properties): MapConfig = {
    val map = new mutable.HashMap[String, Either[String, Map[String, String]]]
    props.forEach((k, v) =>
      Objects.requireNonNull(v) match {
        case s: String              => map.put(k.asInstanceOf[String], Left(s))
        case s: Map[String, String] => map.put(k.asInstanceOf[String], Right(s))
        case _ =>
          throw new UnsupportedClassVersionError(
            s"Only accept the string or Map<String, String>, actual:${v.getClass.getName}")
    })
    new MapConfig(map.toMap)
  }
  def apply(json: OharaJson): MapConfig = try {
    import scala.collection.JavaConverters._
    val map = new mutable.HashMap[String, Either[String, Map[String, String]]]
    ConfigFactory
      .parseReader(new StringReader(json.toString))
      // convert the config to tree in order to get the map type
      .root
      .entrySet()
      .forEach(entry =>
        entry.getValue.unwrapped() match {
          case s: String                        => map.put(entry.getKey, Left(s))
          case s: java.util.Map[String, String] => map.put(entry.getKey, Right(s.asScala.toMap))
      })
    new MapConfig(map.toMap)
  } catch {
    // wrap the typesafe's exception since we don't want to expose the typesafe
    case e: ConfigException => throw new IllegalArgumentException(e)
  }
}
