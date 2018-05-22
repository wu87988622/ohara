package com.island.ohara.config

import java.util.Properties

/**
  * A string-based config collection. If the configures used in the code are the primitve type, this class is a helper tool to be a configuration base.
  * NOTED: the implementations of this class are not required to be thread-safe
  */
trait OharaConfig extends Iterable[(String, Either[String, Map[String, String]])] {

  /**
    * Create a snapshot for this OharaConfig.
    * @return a snapshot of this one
    */
  def snapshot: OharaConfig = {
    val anotherOne = OharaConfig()
    this.foreach {
      case (key, value) =>
        value match {
          case Left(s)  => anotherOne.set(key, s)
          case Right(s) => anotherOne.set(key, s)
        }
    }
    anotherOne
  }

  /**
    * add a string with key
    * @param key key
    * @param value value
    * @return String or Map<String, String> if previous key-value exists. Otherwise None
    */
  def set(key: String, value: String): Option[Either[String, Map[String, String]]]

  /**
    * add a string with key
    * @param key key
    * @param value value
    * @return String or Map<String, String> if previous key-value exists. Otherwise None
    */
  def set(key: String, value: Map[String, String]): Option[Either[String, Map[String, String]]]

  /**
    * add a boolean value with key.
    * NOTED: the default implementation use scala conversion to convert the value to String. If your implementation have another form of value, please provide the your implementation
    * @param key
    * @param value
    * @return String or Map<String, String> if previous key-value exists. Otherwise None
    */
  def set(key: String, value: Boolean): Option[Either[String, Map[String, String]]] = set(key, value.toString)

  /**
    * add a Short value with key.
    * NOTED: the default implementation use scala conversion to convert the value to String. If your implementation have another form of value, please provide the your implementation
    * @param key
    * @param value
    * @return String or Map<String, String> if previous key-value exists. Otherwise None
    */
  def set(key: String, value: Short): Option[Either[String, Map[String, String]]] = set(key, value.toString)

  /**
    * add a Int value with key.
    * NOTED: the default implementation use scala conversion to convert the value to String. If your implementation have another form of value, please provide the your implementation
    * @param key
    * @param value
    * @return String or Map<String, String> if previous key-value exists. Otherwise None
    */
  def set(key: String, value: Int): Option[Either[String, Map[String, String]]] = set(key, value.toString)

  /**
    * add a Long value with key.
    * NOTED: the default implementation use scala conversion to convert the value to String. If your implementation have another form of value, please provide the your implementation
    * @param key
    * @param value
    * @return String or Map<String, String> if previous key-value exists. Otherwise None
    */
  def set(key: String, value: Long): Option[Either[String, Map[String, String]]] = set(key, value.toString)

  /**
    * add a Float value with key.
    * NOTED: the default implementation use scala conversion to convert the value to String. If your implementation have another form of value, please provide the your implementation
    * @param key
    * @param value
    * @return String or Map<String, String> if previous key-value exists. Otherwise None
    */
  def set(key: String, value: Float): Option[Either[String, Map[String, String]]] = set(key, value.toString)

  /**
    * add a Double value with key.
    * NOTED: the default implementation use scala conversion to convert the value to String. If your implementation have another form of value, please provide the your implementation
    * @param key
    * @param value
    * @return String or Map<String, String> if previous key-value exists. Otherwise None
    */
  def set(key: String, value: Double): Option[Either[String, Map[String, String]]] = set(key, value.toString)

  /**
    * Get the value realted to the key of property. If the key-value doesn't exist, this method will return the default value of property.
    * @param prop property
    * @tparam T type
    * @return parsed value or default value
    */
  def get[T](prop: Property[T]): T

  /**
    * @param key key
    * @return String or Map<String, String> if the value mapped to the input key exist. Otherwise None
    */
  def get(key: String): Option[Either[String, Map[String, String]]]

  /**
    * @param prop property
    * @return true if there is a value mapped to the key of property
    */
  def exist(prop: Property[_]): Boolean = exist(prop.key)

  /**
    * @param key key
    * @return true if there is a value mapped to the key
    */
  def exist(key: String): Boolean

  //------------[require]------------//

  /**
    * Get and convert the value to String. If the key doesn't exist, a runtime exception will be thrown.
    * NOTED: the default implementation use scala conversion to convert the value to String. If your implementation have another form of value, please provide the your implementation
    * @param key key
    * @return value
    */
  def requireString(key: String): String =
    get(key)
      .map {
        case Left(s)  => s
        case Right(s) => throw new IllegalArgumentException(s"required: String, actual:${s.getClass.getName}")
      }
      .getOrElse(throw new IllegalArgumentException(s"The $key doesn't exist"))

  /**
    * Get and convert the value to Map<String, String>. If the key doesn't exist, a runtime exception will be thrown.
    * @param key key
    * @return value
    */
  def requireMap(key: String): Map[String, String] =
    get(key)
      .map {
        case Left(s) =>
          throw new IllegalArgumentException(s"required: Map[String, String], actual:${s.getClass.getName}")
        case Right(s) => s
      }
      .getOrElse(throw new IllegalArgumentException(s"The $key doesn't exist"))

  /**
    * Get and convert the value to Short. If the key doesn't exist, a runtime exception will be thrown.
    * NOTED: the default implementation use scala conversion to convert the value to Short. If your implementation have another form of value, please provide the your implementation
    *
    * @param key key
    * @return value
    */
  def requireShort(key: String): Short = requireString(key).toShort

  /**
    * Get and convert the value to Int. If the key doesn't exist, a runtime exception will be thrown.
    * NOTED: the default implementation use scala conversion to convert the value to Int. If your implementation have another form of value, please provide the your implementation
    *
    * @param key key
    * @return value
    */
  def requireInt(key: String): Int = requireString(key).toInt

  /**
    * Get and convert the value to Long. If the key doesn't exist, a runtime exception will be thrown.
    * NOTED: the default implementation use scala conversion to convert the value to Long. If your implementation have another form of value, please provide the your implementation
    *
    * @param key key
    * @return value
    */
  def requireLong(key: String): Long = requireString(key).toLong

  /**
    * Get and convert the value to Float. If the key doesn't exist, a runtime exception will be thrown.
    * NOTED: the default implementation use scala conversion to convert the value to Float. If your implementation have another form of value, please provide the your implementation
    *
    * @param key key
    * @return value
    */
  def requireFloat(key: String): Float = requireString(key).toFloat

  /**
    * Get and convert the value to Double. If the key doesn't exist, a runtime exception will be thrown.
    * NOTED: the default implementation use scala conversion to convert the value to Double. If your implementation have another form of value, please provide the your implementation
    *
    * @param key key
    * @return value
    */
  def requireDouble(key: String): Double = requireString(key).toDouble

  /**
    * Get and convert the value to Boolean. If the key doesn't exist, a runtime exception will be thrown.
    * NOTED: the default implementation use scala conversion to convert the value to Boolean. If your implementation have another form of value, please provide the your implementation
    *
    * @param key key
    * @return value
    */
  def requireBoolean(key: String): Boolean = requireString(key).toBoolean

  /**
    * Convert all configuration to string with json format.
    * NOTED: make sure the output json string consistent.
    * @return a OharaJson
    */
  def toJson: OharaJson

  /**
    * Convert this config to java.util.Properties. Since most kafka components require a Properties in constructing, this helper method can make user life easier
    * @return a Properties have all key-value of this config
    */
  def toProperties: Properties

  /**
    * create a new OharaConfig consising of this OharaConfig and the json content.
    * @param json json
    * @return new OharaConfig consising of this OharaConfig and the json content.
    */
  def merge(json: OharaJson): OharaConfig = merge(OharaConfig(json))

  /**
    * create a new OharaConfig consising of this OharaConfig and the Properties.
    * @param props Properties
    * @return new OharaConfig consising of this OharaConfig and the Properties.
    */
  def merge(props: Properties): OharaConfig = merge(OharaConfig(props))

  /**
    * Merge all configuration of another one with this OharaConfig. The key-value in this OharaConfig will be replaced by another one.
    * @param another another OharaConfig
    * @return an new OharaConfig consisting of another OharaConfig and this one
    */
  def merge(another: OharaConfig): OharaConfig = {
    val copy = snapshot
    copy.load(another)
    copy
  }

  /**
    * load the json to this OharaConfig.
    * @param json json
    * @return this OharaConfig with the json content
    */
  def load(json: OharaJson): OharaConfig = load(OharaConfig(json))

  /**
    * load the Properties to this OharaConfig.
    * @param props Properties
    * @return this OharaConfig with the properties
    */
  def load(props: Properties): OharaConfig = load(OharaConfig(props))

  /**
    * load the OharaConfig to this OharaConfig.
    * @param another another OharaConfig
    * @return this OharaConfig with the extra OharaConfig
    */
  def load(another: OharaConfig): OharaConfig = {
    another.foreach {
      case (key, value) =>
        value match {
          case Left(s)  => set(key, s)
          case Right(s) => set(key, s)
        }
    }
    this
  }
}

object OharaConfig {

  /**
    * @return a empty OharaConfig
    */
  def apply(): OharaConfig = new MapConfig

  /**
    * @param props used to initialize the OharaConfig
    * @return a OharaConfig with same content of props
    */
  def apply(props: Properties): OharaConfig = MapConfig(props)

  /**
    * @param json used to initialize the OharaConfig
    * @return a OharaConfig with same content of json
    */
  def apply(json: OharaJson): OharaConfig = MapConfig(json)

  /**
    * @param config used to initialize the OharaConfig
    * @return a OharaConfig with same content of config
    */
  def apply(config: OharaConfig): OharaConfig = MapConfig(config)
}
