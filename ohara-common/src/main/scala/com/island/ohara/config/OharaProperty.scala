package com.island.ohara.config

/**
  * A option of config.
  * Noted: there are two kind of "from" methods but it is ok to implement one of them.
  *
  * @tparam T the value converted from string config
  */
trait OharaProperty[T] {

  /**
    * key to this property.
    * @return key string
    */
  def key: String

  /**
    * the default value of this property.
    * @return default value
    */
  def default: Option[T]

  /**
    * description to this property.
    * @return description
    */
  def description: String

  /**
    * parse the string to the value.
    * @param value the string value
    * @return value
    */
  protected[config] def from(value: String): T

  /**
    * parse the Map<String, String> to the value.
    * @param value the Map<String, String> value
    * @return value
    */
  protected[config] def from(value: Map[String, String]): T

  override def toString: String = s"key:$key, default:$default, $description"

  /**
    * Require the value related to the key of property. If the key-value doesn't exist, this method will return the default value of property.
    * If the property has no default value, an IllegalArgumentException will be thrown.
    * @param config config
    * @return parsed value or default value
    */
  def require(config: OharaConfig): T =
    get(config).getOrElse(throw new IllegalArgumentException(s"the ${key} doesn't exist. $description"))

  def require(config: T): T =
    if (config == null) default.getOrElse(throw new IllegalArgumentException(s"No valid value. $description"))
    else config

  /**
    * Get the value related to the key of property. If the key-value doesn't exist, this method will return the default value of property.
    * If the property has no default value, an None will be returned
    * @param config config
    * @return parsed value or default value or None
    */
  def get(config: OharaConfig): Option[T] = config
    .get(key)
    .map {
      case Left(s)  => from(s)
      case Right(s) => from(s)
    }
    .orElse(default)

  /**
    * set the value to target config
    * @param config config to be updated
    * @param value update value
    * @return previous value of None
    */
  def set(config: OharaConfig, value: T): Option[T]
}

object OharaProperty {
  def builder = new OharaPropertyBuilder()
}
