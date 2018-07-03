package com.island.ohara.config

import java.io.StringReader
import java.util.{Objects, Properties}

import com.typesafe.config.{ConfigException, ConfigFactory, ConfigRenderOptions, ConfigValueFactory}

import scala.collection.mutable

/**
  * Implemented by typesafe config.
  */
private class MapConfig(another: Map[String, Either[Any, Map[String, Any]]] = null) extends OharaConfig {
  private val config = new mutable.HashMap[String, Either[Any, Map[String, Any]]]()
  if (another != null) config ++= another

  override def exist(key: String): Boolean = config.contains(key)

  private[this] def pairToString(
    pair: (String, Either[Any, Map[String, Any]])): (String, Either[String, Map[String, String]]) =
    (pair._1, valueToString(pair._2))

  private[this] def valueToString(value: Either[Any, Map[String, Any]]) = value match {
    case Left(s) => Left(s.toString)
    case Right(s) =>
      Right(s.map {
        case (k, v) => (k, v.toString)
      })
  }

  override def iterator: Iterator[(String, Either[String, Map[String, String]])] =
    new Iterator[(String, Either[String, Map[String, String]])]() {
      private[this] val iter = config.iterator

      override def hasNext: Boolean = iter.hasNext

      override def next(): (String, Either[String, Map[String, String]]) = pairToString(iter.next())
    }

  private[this] def doSet(key: String, value: Any): Option[Either[String, Map[String, String]]] = {
    val previous = config.get(Objects.requireNonNull(key))
    value match {
      case map: Map[String, String] => config.update(key, Right(Objects.requireNonNull(map)))
      case other: Any               => config.update(key, Left(Objects.requireNonNull(other)))
    }
    previous.map(valueToString(_))
  }

  override def set(key: String, value: String): Option[Either[String, Map[String, String]]] = doSet(key, value)

  override def set(key: String, value: Boolean): Option[Either[String, Map[String, String]]] = doSet(key, value)

  override def set(key: String, value: Short): Option[Either[String, Map[String, String]]] = doSet(key, value)

  override def set(key: String, value: Int): Option[Either[String, Map[String, String]]] = doSet(key, value)

  override def set(key: String, value: Long): Option[Either[String, Map[String, String]]] = doSet(key, value)

  override def set(key: String, value: Float): Option[Either[String, Map[String, String]]] = doSet(key, value)

  override def set(key: String, value: Double): Option[Either[String, Map[String, String]]] = doSet(key, value)

  override def toJson: OharaJson = {
    import scala.collection.JavaConverters._
    var typeSafe = ConfigFactory.empty()
    config.foreach {
      case (key, value) =>
        value match {
          case Left(s)  => typeSafe = typeSafe.withValue(key, ConfigValueFactory.fromAnyRef(s))
          case Right(s) => typeSafe = typeSafe.withValue(key, ConfigValueFactory.fromAnyRef(s.asJava))
        }
    }
    OharaJson(typeSafe.root().render(ConfigRenderOptions.concise()))
  }

  override def get(key: String): Option[Either[String, Map[String, String]]] =
    config.get(Objects.requireNonNull(key)).map(valueToString(_))

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
    doSet(key, value)

  override def toPlainMap: Map[String, String] = config
    .filter {
      case (_, v) => v.isLeft
    }
    .map {
      case (k, v) => (k, v.left.get.toString)
    }
    .toMap

  override def remove(key: String): Option[Either[String, Map[String, String]]] =
    config.remove(key).map(valueToString(_))
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

  def apply(config: OharaConfig): MapConfig = config match {
    case mapConfig: MapConfig => {
      val map = new mutable.HashMap[String, Either[Any, Map[String, Any]]]
      mapConfig.config.foreach {
        case (key, value) => map.put(key, value)
      }
      new MapConfig(map.toMap)
    }
    case _ =>
      throw new UnsupportedOperationException(s"Unsupported to initialize MapConfig by ${config.getClass.getName}")
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
    val map = new mutable.HashMap[String, Either[Any, Map[String, Any]]]
    ConfigFactory
      .parseReader(new StringReader(json.toString))
      // convert the config to tree in order to get the map type
      .root
      .entrySet()
      .forEach(entry =>
        entry.getValue.unwrapped() match {
          case s: java.util.Map[String, String] => map.put(entry.getKey, Right(s.asScala.toMap))
          case s: Object                        => map.put(entry.getKey, Left(s))
      })
    new MapConfig(map.toMap)
  } catch {
    // wrap the typesafe's exception since we don't want to expose the typesafe
    case e: ConfigException => throw new IllegalArgumentException(e)
  }
}
