package com.island.ohara.config

import java.util.Objects

/**
  * used to build the Property. It requires user to assign the 1) key and 2) description. The 1) alias and 2) default value
  * are optional. If you have custom implementation of conversing string to value, the #property(fun) is designed for you.
  * Noted: it is not thread-safe.
  * Noted: call #clear before reusing the builder to construct another property
  */
class OharaPropertyBuilder private[config] {
  private[this] var key: String = _
  private[this] var description: String = _
  private[this] var default: Any = null
  private[this] var alias: String = _

  /**
    * set the key for the property.
    * @param key key of property
    * @return this builder
    */
  def key(key: String): OharaPropertyBuilder = {
    this.key = key
    this
  }

  /**
    * set the description for the property.
    * @param description description of property
    * @return this builder
    */
  def description(description: String): OharaPropertyBuilder = {
    this.description = description
    this
  }

  /**
    * create a property doing nothing abort the conversion.
    * @param default default value
    * @return an new property which can convert the string value to string. If the related key isn't existed, it return the default value
    */
  def stringProperty(default: String): OharaProperty[String] = { this.default = default; stringProperty }

  /**
    * do nothing abort the conversion.
    * @return an new property which can convert the string value to string
    */
  def stringProperty: OharaProperty[String] = property(_.toString)

  /**
    * create a property using scala conversion to parse string value to short.
    * @param default default value
    * @return an new property which can convert the string value to short. If the related key isn't existed, it return the default value
    */
  def shortProperty(default: Short): OharaProperty[Short] = { this.default = default; shortProperty }

  /**
    * create a property using scala conversion to parse string value to short.
    * @return an new property which can convert the string value to short.
    */
  def shortProperty: OharaProperty[Short] = property(_.toShort)

  /**
    * create a property using scala conversion to parse string value to int.
    * @param default default value
    * @return an new property which can convert the string value to int. If the related key isn't existed, it return the default value
    */
  def intProperty(default: Int): OharaProperty[Int] = { this.default = default; intProperty }

  /**
    * create a property using scala conversion to parse string value to int.
    * @return an new property which can convert the string value to int.
    */
  def intProperty: OharaProperty[Int] = property(_.toInt)

  /**
    * create a property using scala conversion to parse string value to long.
    * @param default default value
    * @return an new property which can convert the string value to long. If the related key isn't existed, it return the default value
    */
  def longProperty(default: Long): OharaProperty[Long] = { this.default = default; longProperty }

  /**
    * create a property using scala conversion to parse string value to long.
    * @return an new property which can convert the string value to long.
    */
  def longProperty: OharaProperty[Long] = property(_.toLong)

  /**
    * create a property using scala conversion to parse string value to float.
    * @param default default value
    * @return an new property which can convert the string value to float. If the related key isn't existed, it return the default value
    */
  def floatProperty(default: Float): OharaProperty[Float] = { this.default = default; floatProperty }

  /**
    * create a property using scala conversion to parse string value to float.
    * @return an new property which can convert the string value to float.
    */
  def floatProperty: OharaProperty[Float] = property(_.toFloat)

  /**
    * create a property using scala conversion to parse string value to double.
    * @param default default value
    * @return an new property which can convert the string value to double. If the related key isn't existed, it return the default value
    */
  def doubleProperty(default: Double): OharaProperty[Double] = { this.default = default; doubleProperty }

  /**
    * create a property using scala conversion to parse string value to double.
    * @return an new property which can convert the string value to double.
    */
  def doubleProperty: OharaProperty[Double] = property(_.toDouble)

  /**
    * create a property using scala conversion to parse string value to boolean.
    * @param default default value
    * @return an new property which can convert the string value to boolean. If the related key isn't existed, it return the default value
    */
  def booleanProperty(default: Boolean): OharaProperty[Boolean] = { this.default = default; booleanProperty }

  /**
    * create a property using scala conversion to parse string value to boolean.
    * @return an new property which can convert the string value to boolean.
    */
  def booleanProperty: OharaProperty[Boolean] = property(_.toBoolean)

  /**
    * create a property using custom conversion to parse string value to specific value.
    * @param fun conversion function
    * @param default default value
    * @return an new property which can convert the string value to specific type. If the related key isn't existed, it return the default value
    */
  def property[T](fun: String => T, default: T): OharaProperty[T] = {
    this.default = default; property(fun)
  }

  /**
    * create a property using custom conversion to parse string value to specific value.
    * @param fun conversion function
    * @param default default value
    * @return an new property which can convert the string value to specific type. If the related key isn't existed, it return the default value
    */
  def property[T](fun: String => T, fun2: T => String, default: T): OharaProperty[T] = {
    this.default = default; property(fun, fun2)
  }

  /**
    * create a property using custom conversion to parse string value to specific value.
    * @param fun conversion function
    * @return an new property which can convert the string value to specific type.
    */
  def property[T](fun: String => T, fun2: T => String = null): OharaProperty[T] = {
    checkArguments()
    new OharaProperty[T] {
      override def key: String = OharaPropertyBuilder.this.key

      override def default: Option[T] =
        if (OharaPropertyBuilder.this.default == null) None else Some(OharaPropertyBuilder.this.default.asInstanceOf[T])

      override def description: String = OharaPropertyBuilder.this.description

      override protected[config] def from(value: Map[String, String]): T = throw new UnsupportedOperationException(
        "Unsupported to pass Map<String, String>")

      override def set(config: OharaConfig, value: T): Option[T] = {
        val previous = get(config)
        config.set(key, if (fun2 != null) fun2(value) else value)
        previous
      }

      override protected[config] def from(value: String): T = fun(value)
    }
  }

  def mapProperty(default: Map[String, String]): OharaProperty[Map[String, String]] = {
    this.default = default
    mapProperty
  }

  /**
    * create a property without conversion
    * @return an new property
    */
  def mapProperty: OharaProperty[Map[String, String]] = mapProperty(v => v, v => v)

  /**
    * create a property using custom conversion to parse Map[String, String] to specific value.
    * @param fun conversion function
    * @param default default value
    * @return an new property which can convert the Map[String, String] value to specific type. If the related key isn't existed, it return the default value
    */
  def mapProperty[T](fun: String => T, fun2: T => String, default: Map[String, T]): OharaProperty[Map[String, T]] = {
    this.default = default
    mapProperty(fun, fun2)
  }

  /**
    * create a property using custom conversion to parse Map[String, String] to specific value.
    * @param fun conversion function
    * @return an new property which can convert the Map[String, String] value to specific type.
    */
  def mapProperty[T](fun: String => T, fun2: T => String): OharaProperty[Map[String, T]] = {
    checkArguments()
    new OharaProperty[Map[String, T]] {
      override def key: String = OharaPropertyBuilder.this.key

      override def default: Option[Map[String, T]] =
        if (OharaPropertyBuilder.this.default == null) None
        else Some(OharaPropertyBuilder.this.default.asInstanceOf[Map[String, T]])

      override def description: String = OharaPropertyBuilder.this.description

      override protected[config] def from(value: String): Map[String, T] = throw new UnsupportedOperationException(
        "Unsupported to pass String")

      override protected[config] def from(value: Map[String, String]): Map[String, T] = value.map {
        case (k, v) => (k, fun(v))
      }

      override def set(config: OharaConfig, value: Map[String, T]): Option[Map[String, T]] = {
        val previous = get(config)
        config.set(key, value.map {
          case (k, v) => (k, fun2(v))
        })
        previous
      }
    }
  }

  private[this] def checkArguments(): Unit = {
    Objects.requireNonNull(key)
    if (description == null) description = key
    Objects.requireNonNull(description)
    if (alias == null) alias = key
    Objects.requireNonNull(alias)
  }

  /**
    * set all inner members to null. this method works for the users who plan to reuse this builder to construct another property
    * @return this build
    */
  def clear(): OharaPropertyBuilder = {
    this.default = null
    this.alias = null
    this.key = null
    this.description = null
    this
  }
}
