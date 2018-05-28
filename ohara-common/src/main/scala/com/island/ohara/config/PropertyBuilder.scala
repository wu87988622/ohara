package com.island.ohara.config

import java.util.Objects

/**
  * used to build the Property. It requires user to assign the 1) key and 2) description. The 1) alias and 2) default value
  * are optional. If you have custom implementation of conversing string to value, the #property(fun) is designed for you.
  * Noted: it is not thread-safe.
  * Noted: call #clear before reusing the builder to construct another property
  */
class PropertyBuilder private {
  private[this] var key: String = _
  private[this] var description: String = _
  private[this] var default: Any = null
  private[this] var alias: String = _

  /**
    * set the key for the property.
    * @param key key of property
    * @return this builder
    */
  def key(key: String): PropertyBuilder = {
    this.key = key
    this
  }

  /**
    * set the description for the property.
    * @param description description of property
    * @return this builder
    */
  def description(description: String): PropertyBuilder = {
    this.description = description
    this
  }

  /**
    * set the alias for the property.
    * @param alias alias of property
    * @return this builder
    */
  def alias(alias: String): PropertyBuilder = {
    this.alias = alias
    this
  }

  /**
    * create a property doing nothing abort the conversion.
    * @param default default value
    * @return an new property which can convert the string value to string. If the related key isn't existed, it return the default value
    */
  def stringProperty(default: String): Property[String] = { this.default = default; stringProperty }

  /**
    * do nothing abort the conversion.
    * @return an new property which can convert the string value to string
    */
  def stringProperty: Property[String] = property(_.toString, _.toString)

  /**
    * create a property using scala conversion to parse string value to short.
    * @param default default value
    * @return an new property which can convert the string value to short. If the related key isn't existed, it return the default value
    */
  def shortProperty(default: Short): Property[Short] = { this.default = default; shortProperty }

  /**
    * create a property using scala conversion to parse string value to short.
    * @return an new property which can convert the string value to short.
    */
  def shortProperty: Property[Short] = property(_.toShort, _.toString)

  /**
    * create a property using scala conversion to parse string value to int.
    * @param default default value
    * @return an new property which can convert the string value to int. If the related key isn't existed, it return the default value
    */
  def intProperty(default: Int): Property[Int] = { this.default = default; intProperty }

  /**
    * create a property using scala conversion to parse string value to int.
    * @return an new property which can convert the string value to int.
    */
  def intProperty: Property[Int] = property(_.toInt, _.toString)

  /**
    * create a property using scala conversion to parse string value to long.
    * @param default default value
    * @return an new property which can convert the string value to long. If the related key isn't existed, it return the default value
    */
  def longProperty(default: Long): Property[Long] = { this.default = default; longProperty }

  /**
    * create a property using scala conversion to parse string value to long.
    * @return an new property which can convert the string value to long.
    */
  def longProperty: Property[Long] = property(_.toLong, _.toString)

  /**
    * create a property using scala conversion to parse string value to float.
    * @param default default value
    * @return an new property which can convert the string value to float. If the related key isn't existed, it return the default value
    */
  def floatProperty(default: Float): Property[Float] = { this.default = default; floatProperty }

  /**
    * create a property using scala conversion to parse string value to float.
    * @return an new property which can convert the string value to float.
    */
  def floatProperty: Property[Float] = property(_.toFloat, _.toString)

  /**
    * create a property using scala conversion to parse string value to double.
    * @param default default value
    * @return an new property which can convert the string value to double. If the related key isn't existed, it return the default value
    */
  def doubleProperty(default: Double): Property[Double] = { this.default = default; doubleProperty }

  /**
    * create a property using scala conversion to parse string value to double.
    * @return an new property which can convert the string value to double.
    */
  def doubleProperty: Property[Double] = property(_.toDouble, _.toString)

  /**
    * create a property using scala conversion to parse string value to boolean.
    * @param default default value
    * @return an new property which can convert the string value to boolean. If the related key isn't existed, it return the default value
    */
  def booleanProperty(default: Boolean): Property[Boolean] = { this.default = default; booleanProperty }

  /**
    * create a property using scala conversion to parse string value to boolean.
    * @return an new property which can convert the string value to boolean.
    */
  def booleanProperty: Property[Boolean] = property(_.toBoolean, _.toString)

  /**
    * create a property using custom conversion to parse string value to specific value.
    * @param fun conversion function
    * @param default default value
    * @return an new property which can convert the string value to specific type. If the related key isn't existed, it return the default value
    */
  def property[T](fun: String => T, fun2: T => String, default: T): Property[T] = {
    this.default = default; property(fun, fun2)
  }

  /**
    * create a property using custom conversion to parse string value to specific value.
    * @param fun conversion function
    * @return an new property which can convert the string value to specific type.
    */
  def property[T](fun: String => T, fun2: T => String): Property[T] = {
    checkArguments()
    new Property[T] {
      override def key: String = PropertyBuilder.this.key

      override def alias: String = PropertyBuilder.this.alias

      override def default: Option[T] =
        if (PropertyBuilder.this.default == null) None else Some(PropertyBuilder.this.default.asInstanceOf[T])

      override def description: String = PropertyBuilder.this.description

      override protected[config] def from(value: Map[String, String]): T = throw new UnsupportedOperationException(
        "Unsupported to pass Map<String, String>")

      override def set(config: OharaConfig, value: T): Option[T] = {
        val previous = get(config)
        config.set(key, fun2(value))
        previous
      }

      override protected[config] def from(value: String): T = fun(value)
    }
  }

  def mapProperty(default: Map[String, String]): Property[Map[String, String]] = {
    this.default = default
    mapProperty
  }

  /**
    * create a property without conversion
    * @return an new property
    */
  def mapProperty: Property[Map[String, String]] = mapProperty(v => v, v => v)

  /**
    * create a property using custom conversion to parse Map[String, String] to specific value.
    * @param fun conversion function
    * @param default default value
    * @return an new property which can convert the Map[String, String] value to specific type. If the related key isn't existed, it return the default value
    */
  def mapProperty[T](fun: String => T, fun2: T => String, default: Map[String, T]): Property[Map[String, T]] = {
    this.default = default
    mapProperty(fun, fun2)
  }

  /**
    * create a property using custom conversion to parse Map[String, String] to specific value.
    * @param fun conversion function
    * @return an new property which can convert the Map[String, String] value to specific type.
    */
  def mapProperty[T](fun: String => T, fun2: T => String): Property[Map[String, T]] = {
    checkArguments()
    new Property[Map[String, T]] {
      override def key: String = PropertyBuilder.this.key

      override def alias: String = PropertyBuilder.this.alias

      override def default: Option[Map[String, T]] =
        if (PropertyBuilder.this.default == null) None
        else Some(PropertyBuilder.this.default.asInstanceOf[Map[String, T]])

      override def description: String = PropertyBuilder.this.description

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
  def clear(): PropertyBuilder = {
    this.default = null
    this.alias = null
    this.key = null
    this.description = null
    this
  }
}

private object PropertyBuilder {
  def apply() = new PropertyBuilder()
}
