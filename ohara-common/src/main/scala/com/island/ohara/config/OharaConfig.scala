package com.island.ohara.config

import java.util.Properties

/**
  * A string-based config collection. If the configures used in the code are the primitve type, this class is a helper tool to be a configuration base.
  * NOTED: the implementations of this class are not required to be thread-safe
  */
trait OharaConfig extends Iterable[(String, String)] {

  /**
    * Create a snapshot for this OharaConfig.
    * @return a snapshot of this one
    */
  def snapshot: OharaConfig = {
    val anotherOne = OharaConfig()
    this.foreach {
      case (key, value) => anotherOne.set(key, value)
    }
    anotherOne
  }

  /**
    * add a string with key
    * @param key key
    * @param value value
    * @return Some[String] if previous key-value exists. Otherwise None
    */
  def set(key: String, value: String): Option[String]

  /**
    * add a boolean value with key.
    * NOTED: the default implementation use scala conversion to convert the value to String. If your implemetation have another form of value, please provide the your implementation
    * @param key
    * @param value
    * @return Some[Boolean] if previous key-value exists. Otherwise None
    */
  def set(key: String, value: Boolean): Option[Boolean] = set(key, value.toString).map(_.toBoolean)

  /**
    * add a Short value with key.
    * NOTED: the default implementation use scala conversion to convert the value to String. If your implemetation have another form of value, please provide the your implementation
    * @param key
    * @param value
    * @return Some[Short] if previous key-value exists. Otherwise None
    */
  def set(key: String, value: Short): Option[Short] = set(key, value.toString).map(_.toShort)

  /**
    * add a Int value with key.
    * NOTED: the default implementation use scala conversion to convert the value to String. If your implemetation have another form of value, please provide the your implementation
    * @param key
    * @param value
    * @return Some[Int] if previous key-value exists. Otherwise None
    */
  def set(key: String, value: Int): Option[Int] = set(key, value.toString).map(_.toInt)

  /**
    * add a Long value with key.
    * NOTED: the default implementation use scala conversion to convert the value to String. If your implemetation have another form of value, please provide the your implementation
    * @param key
    * @param value
    * @return Some[Long] if previous key-value exists. Otherwise None
    */
  def set(key: String, value: Long): Option[Long] = set(key, value.toString).map(_.toLong)

  /**
    * add a Float value with key.
    * NOTED: the default implementation use scala conversion to convert the value to String. If your implemetation have another form of value, please provide the your implementation
    * @param key
    * @param value
    * @return Some[Float] if previous key-value exists. Otherwise None
    */
  def set(key: String, value: Float): Option[Float] = set(key, value.toString).map(_.toFloat)

  /**
    * add a Double value with key.
    * NOTED: the default implementation use scala conversion to convert the value to String. If your implemetation have another form of value, please provide the your implementation
    * @param key
    * @param value
    * @return Some[Double] if previous key-value exists. Otherwise None
    */
  def set(key: String, value: Double): Option[Double] = set(key, value.toString).map(_.toDouble)

  /**
    * Get the value realted to the key of property. If the key-value doesn't exist, this method will return the default value of property.
    * @param prop property
    * @tparam T type
    * @return parsed value or default value
    */
  def get[T](prop: Property[T]): T

  /**
    * @param key key
    * @return Some[String] if the value mapped to the input key exist. Otherwise None
    */
  def get(key: String): Option[String]

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
    * NOTED: the default implementation use scala conversion to convert the value to String. If your implemetation have another form of value, please provide the your implementation
    * @param key key
    * @return value
    */
  def requireString(key: String): String =
    get(key).getOrElse(throw new IllegalArgumentException(s"The $key doesn't exist"))

  /**
    * Get and convert the value to Short. If the key doesn't exist, a runtime exception will be thrown.
    * NOTED: the default implementation use scala conversion to convert the value to Short. If your implemetation have another form of value, please provide the your implementation
    *
    * @param key key
    * @return value
    */
  def requireShort(key: String): Short =
    get(key).map(_.toShort).getOrElse(throw new IllegalArgumentException(s"The $key doesn't exist"))

  /**
    * Get and convert the value to Int. If the key doesn't exist, a runtime exception will be thrown.
    * NOTED: the default implementation use scala conversion to convert the value to Int. If your implemetation have another form of value, please provide the your implementation
    *
    * @param key key
    * @return value
    */
  def requireInt(key: String): Int =
    get(key).map(_.toInt).getOrElse(throw new IllegalArgumentException(s"The $key doesn't exist"))

  /**
    * Get and convert the value to Long. If the key doesn't exist, a runtime exception will be thrown.
    * NOTED: the default implementation use scala conversion to convert the value to Long. If your implemetation have another form of value, please provide the your implementation
    *
    * @param key key
    * @return value
    */
  def requireLong(key: String): Long =
    get(key).map(_.toLong).getOrElse(throw new IllegalArgumentException(s"The $key doesn't exist"))

  /**
    * Get and convert the value to Float. If the key doesn't exist, a runtime exception will be thrown.
    * NOTED: the default implementation use scala conversion to convert the value to Float. If your implemetation have another form of value, please provide the your implementation
    *
    * @param key key
    * @return value
    */
  def requireFloat(key: String): Float =
    get(key).map(_.toFloat).getOrElse(throw new IllegalArgumentException(s"The $key doesn't exist"))

  /**
    * Get and convert the value to Double. If the key doesn't exist, a runtime exception will be thrown.
    * NOTED: the default implementation use scala conversion to convert the value to Double. If your implemetation have another form of value, please provide the your implementation
    *
    * @param key key
    * @return value
    */
  def requireDouble(key: String): Double =
    get(key).map(_.toDouble).getOrElse(throw new IllegalArgumentException(s"The $key doesn't exist"))

  /**
    * Get and convert the value to Boolean. If the key doesn't exist, a runtime exception will be thrown.
    * NOTED: the default implementation use scala conversion to convert the value to Boolean. If your implemetation have another form of value, please provide the your implementation
    *
    * @param key key
    * @return value
    */
  def requireBoolean(key: String): Boolean =
    get(key).map(_.toBoolean).getOrElse(throw new IllegalArgumentException(s"The $key doesn't exist"))

  /**
    * Convert all configuration to string with json format.
    * NOTED: make sure the output json string consistent.
    * @return a OharaJson
    */
  def toJson: OharaJson

  /**
    * Convert this config to java.util.Properties. Since most kafka conpoments require a Properties in constructing, this helper method can make user life easier
    * @return a Properties have all key-value of thie config
    */
  def toProperties: Properties

  /**
    * create a new OharaConfig consising of this OharaConfig and the json content.
    * @param json json
    * @return new OharaConfig consising of this OharaConfig and the json content.
    */
  def merge(json: OharaJson): OharaConfig

  /**
    * create a new OharaConfig consising of this OharaConfig and the Properties.
    * @param props Properties
    * @return new OharaConfig consising of this OharaConfig and the Properties.
    */
  def merge(props: Properties): OharaConfig = {
    val copy = snapshot
    props.forEach((k, v) => copy.set(k.asInstanceOf[String], v.asInstanceOf[String]))
    copy
  }

  /**
    * Merge all configuration of another one with this OharaConfig. The key-value in this OharaConfig will be replaced by another one.
    * @param another another OharaConfig
    * @return an new OharaConfig consisting of another OharaConfig and this one
    */
  def merge(another: OharaConfig): OharaConfig = {
    val copy = snapshot
    another.foreach {
      case (key, value) => copy.set(key, value)
    }
    copy
  }

  /**
    * load the json to this OharaConfig.
    * @param json json
    * @return this OharaConfig with the json content
    */
  def load(json: OharaJson): OharaConfig

  /**
    * load the Properties to this OharaConfig.
    * @param props Properties
    * @return this OharaConfig with the properties
    */
  def load(props: Properties): OharaConfig = {
    props.forEach((k, v) => set(k.asInstanceOf[String], v.asInstanceOf[String]))
    this
  }

  /**
    * load the OharaConfig to this OharaConfig.
    * @param another another OharaConfig
    * @return this OharaConfig with the extra OharaConfig
    */
  def load(another: OharaConfig): OharaConfig = {
    another.foreach {
      case (key, value) => set(key, value)
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
